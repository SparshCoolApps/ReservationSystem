package com.example.reservationservice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.context.config.annotation.RefreshScope;
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
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.util.Collection;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Stream;

@IntegrationComponentScan
@EnableBinding({Sink.class, Source.class})
@SpringBootApplication
public class ReservationServiceApplication {

    @Bean
    public CommandLineRunner commandLineRunner(ReservationRepository pr, ReservationCountLimit reservationCountLimit) {
        return args -> {
            final String[] locations = {"Delhi", "Mumbai", "Pune", "Bangalore", "Patna", "Lucknow", "Kanpur", "Agra"};
            Random random = new Random(8);
            Stream.of("Ashish", "Amit", "Amar", "Tushar").
                    forEach(pname -> pr.save(new Reservation(pname, locations[random.nextInt(8)], random.nextInt(reservationCountLimit.getReservationCount()))));

        };
    }

    public static void main(String[] args) {
        SpringApplication.run(ReservationServiceApplication.class, args);
    }
}

@MessageEndpoint
class ReservationProcessor {

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private Source source;

    @ServiceActivator(inputChannel = Sink.INPUT)
    public void acceptRejectReservation(String msg) {

        String msgParts[] = msg.trim().split(",");

        // Message from APIGateway
        if (msgParts[0].equalsIgnoreCase("RESERVATION_MASTER_SAVE")) {
            Reservation s = this.reservationRepository.save(new Reservation(msgParts[1], msgParts[2], Integer.parseInt(msgParts[3])));
        }

        // Message from Booking Service
        // Msg format is RESERVATION_SAVE,<Reservation_Name>,<Reservation_Location>,<Reservation_Count>,<Reservation_TransactionID>
        if (msgParts[0].equalsIgnoreCase("RESERVATION_SAVE")) {

            // Save the Reservation Transaction in Reservation DB
            this.reservationRepository.save(new Reservation(msgParts[1], msgParts[2], Integer.parseInt(msgParts[3]), msgParts[4]));

            // Send a Message for Ticket Service
            // Msg format is RESERVATION_SAVED,<Reservation_Name>,<Reservation_Location>,<Reservation_Count>,<Reservation_TransactionID>
            Message<String> message = MessageBuilder.withPayload("RESERVATION_SAVED" + ","
                    + msgParts[1] + ","
                    + msgParts[2] + ","
                    + msgParts[3] + ","
                    + msgParts[4])
                    .build();
            source.output().send(message);
        }

        // For Rollback operation
        if (msgParts[0].equalsIgnoreCase("RESERVATION_DELETE")) {
            this.reservationRepository.delete(new Reservation(msgParts[1], msgParts[2], Integer.parseInt(msgParts[3])));
        }


    }

}

@RepositoryRestResource
interface ReservationRepository extends JpaRepository<Reservation, Long> {

    @RestResource(path = "by-name")
    Collection<Reservation> findByReservationName(@Param("pn") String reservationName);

}

@RefreshScope
@Component
class ReservationCountLimit {

    @Value("${reservationCount}")
    private int reservationCount;

    public int getReservationCount() {
        return reservationCount;
    }
}

@RefreshScope
@RestController
class MessageRestController {

    @Value("${reservationCount}")
    private int reservationCount;

    @Value("${message}")
    private String msg;

    @GetMapping("/message")
    public String message() {
        return this.msg;
    }

    @GetMapping("/limit")
    public String limit() {
        return "Limit is " + this.reservationCount;
    }

}

@Entity
class Reservation {

    @Id
    @GeneratedValue
    private long id;

    private String reservationName;
    private String reservationLocation;
    private int reservationCount;
    private String reservationTransactionID;

    public Reservation(String reservationName, String reservationLocation, int reservationCount, String reservationTransactionID) {
        this.reservationName = reservationName;
        this.reservationLocation = reservationLocation;
        this.reservationCount = reservationCount;
        this.reservationTransactionID = reservationTransactionID;
    }

    public Reservation(String reservationName, String reservationLocation, int reservationCount) {
        this.reservationName = reservationName;
        this.reservationLocation = reservationLocation;
        this.reservationCount = reservationCount;
        this.reservationTransactionID = UUID.randomUUID().toString();
    }

    public Reservation() {
        //for JPA
    }

    public String getReservationName() {
        return reservationName;
    }

    public String getReservationLocation() {
        return reservationLocation;
    }

    public int getReservationCount() {
        return reservationCount;
    }

    public String getReservationTransactionID() {
        return reservationTransactionID;
    }


}



