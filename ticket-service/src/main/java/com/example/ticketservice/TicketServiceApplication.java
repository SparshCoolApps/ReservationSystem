package com.example.ticketservice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
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

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.util.Random;
import java.util.stream.Stream;

@IntegrationComponentScan
@EnableBinding({Sink.class, Source.class})
@SpringBootApplication
public class TicketServiceApplication {

    @Bean
    public CommandLineRunner commandLineRunner(TicketRepository tr) {
        return args -> {
            final Random random = new Random(500);
            Stream.of("Delhi", "Mumbai", "Pune", "Bangalore", "Patna", "Lucknow", "Kanpur", "Agra").
                    forEach(pname -> tr.save(new Ticket(pname, random.nextInt(500))));

        };
    }

    public static void main(String[] args) {
        SpringApplication.run(TicketServiceApplication.class, args);
    }
}


@MessageEndpoint
class TicketProcessor {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private Source source;

    @ServiceActivator(inputChannel = Sink.INPUT)
    public void acceptTicketLocation(String msg) {

        String msgParts[] = msg.trim().split(",");

        if (msgParts[0].equalsIgnoreCase("TICKET_SAVE")) {
            this.ticketRepository.save(new Ticket(msgParts[1], Integer.parseInt(msgParts[2])));
        }

        // Send a Message for Ticket Service
        // Msg format is RESERVATION_SAVED,<Reservation_Name>,<Reservation_Location>,<Reservation_Count>,<Reservation_TransactionID>
        if (msgParts[0].trim().equalsIgnoreCase("RESERVATION_SAVED")) { //RESERVATION_SAVED
            Ticket inboundTicket = new Ticket(msgParts[2], Integer.parseInt(msgParts[3]));
            Ticket existingTicket = ticketRepository.findByTicketLocation(msgParts[2].trim());

            System.out.println("-------------------------------------");
            System.out.println("Inbound Ticket Details--->");
            System.out.println("Location:: " + inboundTicket.getTicketLocation() + " Counts :: " + inboundTicket.getTicketCounts());
            System.out.println("-------------------------------------");

            if (existingTicket != null) {
                System.out.println("-------------------------------------");
                System.out.println("Existing Ticket Details--->");
                System.out.println("Location:: " + existingTicket.getTicketLocation() + " Counts :: " + existingTicket.getTicketCounts());
                System.out.println("-------------------------------------");
            }

            if (existingTicket != null) {
                if (inboundTicket.getTicketLocation().equalsIgnoreCase(existingTicket.getTicketLocation())) {
                    inboundTicket.setId(existingTicket.getId());
                    if (existingTicket.getTicketCounts() - inboundTicket.getTicketCounts() > 0) {
                        inboundTicket.setTicketCounts(existingTicket.getTicketCounts() - inboundTicket.getTicketCounts());
                        this.ticketRepository.save(inboundTicket);
                    }
                }
            }
            // Save Reservation
            // Event Name is RESERVATION_SAVE
            // Msg format is RESERVATION_SAVE,<Reservation_Name>,<Reservation_Location>,<Reservation_Count>,<TransID>
            Message<String> message = MessageBuilder.withPayload("TICKET_RESERVE" + ","
                    + msgParts[1] + ","
                    + msgParts[2] + ","
                    + msgParts[3] + ","
                    + msgParts[4])
                    .build();
            source.output().send(message);

        }
    }

}


@RepositoryRestResource
interface TicketRepository extends JpaRepository<Ticket, Long> {

    @RestResource(path = "by-location")
    Ticket findByTicketLocation(@Param("location") String ticketLocation);
}


@Entity
class Ticket {

    @Id
    @GeneratedValue
    private Long id;
    private String ticketLocation;
    private int ticketCounts;

    public Ticket(String ticketLocation, int ticketCounts) {
        this.ticketLocation = ticketLocation;
        this.ticketCounts = ticketCounts;
    }

    public Ticket() {

    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setTicketLocation(String ticketLocation) {
        this.ticketLocation = ticketLocation;
    }

    public void setTicketCounts(int ticketCounts) {
        this.ticketCounts = ticketCounts;
    }

    public String getTicketLocation() {
        return ticketLocation;
    }

    public int getTicketCounts() {
        return ticketCounts;
    }
}
