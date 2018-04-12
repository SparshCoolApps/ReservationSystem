package com.example.bookingservice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.cloud.stream.messaging.Source;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.bind.annotation.*;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.util.Date;
import java.util.UUID;

@EnableDiscoveryClient
@IntegrationComponentScan
@EnableBinding({Source.class, Sink.class})
@SpringBootApplication
public class BookingServiceApplication {
    @Bean
    public CommandLineRunner commandLineRunner() {

        return strings -> {
        };
    }

    public static void main(String[] args) {
        SpringApplication.run(BookingServiceApplication.class, args);

    }
}

enum BOOKING_STATUS {
    PENDING, COMPLETED, REJECTED;
}


@RestController
@RequestMapping("/booking")
class BookingService {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private Source source;

    @GetMapping("/getstatus/{transactionID}")
    public Booking getStatus(@PathVariable String transactionID) {
        return bookingRepository.findByBookingTransactionID(transactionID);

    }

    @PostMapping("/ticket")
    public String bookTicket(@RequestBody Reservation r) {

        final String transID = UUID.randomUUID().toString();
        bookingRepository.save(new Booking(r.getReservationName(),
                r.getReservationLocation(), r.getReservationCount(),
                transID));

        // Save Reservation
        // Event Name is RESERVATION_SAVE
        // Msg format is RESERVATION_SAVE,<Reservation_Name>,<Reservation_Location>,<Reservation_Count>,<TransID>
        Message<String> message = MessageBuilder.withPayload("RESERVATION_SAVE" + ","
                + r.getReservationName() + ","
                + r.getReservationLocation() + ","
                + r.getReservationCount() + ","
                + transID)
                .build();
        source.output().send(message);

        return transID;
    }
}

@MessageEndpoint
class BookingProcessor {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private Source source;

    @ServiceActivator(inputChannel = Sink.INPUT)
    public void acceptRejectBooking(String msg) {

        // Msg Format is DOBOOKING, <Reservation_Name>,<Reservation_Location>,<Reservation_Count>,<TransID>
        String msgParts[] = msg.split(",");
        if (msgParts[0].trim().equalsIgnoreCase("DOBOOKING")) {
            bookingRepository.save(new Booking(msgParts[1],
                    msgParts[2], Integer.parseInt(msgParts[3]),
                    msgParts[4]));

            // Save Reservation
            // Event Name is RESERVATION_SAVE
            // Msg format is RESERVATION_SAVE,<Reservation_Name>,<Reservation_Location>,<Reservation_Count>,<TransID>
            Message<String> message = MessageBuilder.withPayload("RESERVATION_SAVE" + ","
                    + msgParts[1] + ","
                    + msgParts[2] + ","
                    + msgParts[3] + ","
                    + msgParts[4])
                    .build();
            source.output().send(message);
        }

        if (msgParts[0].trim().equalsIgnoreCase("TICKET_RESERVE")) {
            Booking inboundBooing = new Booking(msgParts[1], msgParts[2], Integer.parseInt(msgParts[3]), msgParts[4],
                    new Date(System.currentTimeMillis()), BOOKING_STATUS.COMPLETED.name());
            Booking existingBooking = bookingRepository.findByBookingTransactionID(msgParts[4].trim());
            if (existingBooking != null) {
                if (existingBooking.getBookingTransactionID().equalsIgnoreCase(inboundBooing.getBookingTransactionID())) {
                    inboundBooing.setId(existingBooking.getId());
                    bookingRepository.save(inboundBooing);
                }
            }
        }
    }

}


@RepositoryRestResource
interface BookingRepository extends JpaRepository<Booking, Long> {
    @RestResource(path = "by-transactionID")
    Booking findByBookingTransactionID(@Param("tid") String bookingTransactionID);

}


@Entity
class Booking {

    @Id
    @GeneratedValue
    private Long id;

    private String bookingTransactionID;
    private Date bookingDate;
    private String bookingName;
    private String bookingLocation;
    private int bookingSeats;
    private String bookingStatus;

    public Booking() {

    }

    public Booking(String bookingName, String bookingLocation, int bookingSeats, String bookingTransactionID) {
        this.bookingName = bookingName;
        this.bookingLocation = bookingLocation;
        this.bookingSeats = bookingSeats;
        this.bookingTransactionID = bookingTransactionID;
        this.bookingDate = new Date(System.currentTimeMillis());
        this.bookingStatus = BOOKING_STATUS.PENDING.name();
    }

    public Booking(String bookingName, String bookingLocation, int bookingSeats, String bookingTransactionID, Date currentDate, String bookingStatus) {
        this(bookingName, bookingLocation, bookingSeats, bookingTransactionID);
        this.bookingDate = currentDate;
        this.bookingStatus = bookingStatus;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setBookingTransactionID(String bookingTransactionID) {
        this.bookingTransactionID = bookingTransactionID;
    }

    public void setBookingDate(Date bookingDate) {
        this.bookingDate = bookingDate;
    }

    public void setBookingName(String bookingName) {
        this.bookingName = bookingName;
    }

    public void setBookingLocation(String bookingLocation) {
        this.bookingLocation = bookingLocation;
    }

    public void setBookingSeats(int bookingSeats) {
        this.bookingSeats = bookingSeats;
    }

    public void setBookingStatus(String bookingStatus) {
        this.bookingStatus = bookingStatus;
    }

    public Long getId() {
        return id;
    }

    public String getBookingTransactionID() {
        return bookingTransactionID;
    }

    public Date getBookingDate() {
        return bookingDate;
    }

    public String getBookingName() {
        return bookingName;
    }

    public String getBookingLocation() {
        return bookingLocation;
    }

    public int getBookingSeats() {
        return bookingSeats;
    }

    public String getBookingStatus() {
        return bookingStatus;
    }
}

// Data Transfer Object DTO for Reservation
class Reservation {

    private String reservationName;
    private String reservationLocation;
    private int reservationCount;

    public Reservation() {

    }

    public Reservation(String reservationName, String reservationLocation, int reservationCount) {
        this.reservationName = reservationName;
        this.reservationLocation = reservationLocation;
        this.reservationCount = reservationCount;
    }

    public String getReservationName() {
        return this.reservationName;
    }

    public String getReservationLocation() {
        return reservationLocation;
    }

    public int getReservationCount() {
        return reservationCount;
    }
}


// Data Transfer Object DTO for Ticket
class Ticket {

    private String ticketLocation;
    private int ticketCounts;

    public Ticket() {

    }

    public Ticket(String ticketLocation, int ticketCounts) {
        this.ticketLocation = ticketLocation;
        this.ticketCounts = ticketCounts;
    }

    public String getTicketLocation() {
        return ticketLocation;
    }

    public int getTicketCounts() {
        return ticketCounts;
    }
}
