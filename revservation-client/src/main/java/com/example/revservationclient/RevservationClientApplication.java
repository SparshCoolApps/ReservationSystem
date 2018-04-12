package com.example.revservationclient;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.messaging.Source;
import org.springframework.context.annotation.Bean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@EnableBinding(Source.class)
@EnableCircuitBreaker
@EnableZuulProxy
@EnableDiscoveryClient
@SpringBootApplication
public class RevservationClientApplication {

    public static void main(String[] args) {

        SpringApplication.run(RevservationClientApplication.class, args);
    }

    @Bean
    @LoadBalanced
    RestTemplate restTemplate() {
        return new RestTemplate();
    }
}


@RestController
@RequestMapping("api")
@MessageEndpoint
class ReservationAPIGateway {


    private final RestTemplate restTemplate;

    @Autowired
    public ReservationAPIGateway(@LoadBalanced RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Autowired
    private Source source;

    @PostMapping("/reservations")
    public void writeReservation(@RequestBody Reservation r) {
        // Event Name is RESERVATION_MASTER_SAVE
        // Msg format is RESERVATION_MASTER_SAVE,<Reservation_Name>,<Reservation_Location>,<Reservation_Count>
        // Msg for ReservationService
        Message<String> message = MessageBuilder.withPayload("RESERVATION_MASTER_SAVE" + ","
                + r.getReservationName() + ","
                + r.getReservationLocation() + ","
                + r.getReservationCount())
                .build();
        source.output().send(message);
    }

    public Collection<Reservation> getPassangerListFallBack() {
        List<Reservation> defaultReservations = new ArrayList<>();
        defaultReservations.add(new Reservation("SureshaAent", "Pune", 5, UUID.randomUUID().toString()));
        defaultReservations.add(new Reservation("NeerajAgent", "Banaglore", 5, UUID.randomUUID().toString()));
        return defaultReservations;

        // here one can implement the user specific cache requierments

    }

    @HystrixCommand(fallbackMethod = "getPassangerListFallBack")
    @GetMapping("/reservations")
    public Collection<Reservation> getPassangerList() {

        ParameterizedTypeReference<Resources<Reservation>> ptr = new ParameterizedTypeReference<Resources<Reservation>>() {
        };
        ResponseEntity<Resources<Reservation>> responseEntity =
                this.restTemplate.exchange("http://reservation-service/reservations", HttpMethod.GET, null, ptr);
        Collection<Reservation> data = responseEntity.getBody().getContent();
        return data;

    }

    @PostMapping("/tickets")
    public void addLocation(@RequestBody Ticket ticket) {
        // Event Name is TICKET_SAVE
        // Msg is TICKET_SAVE,<Ticket_Location>,<Ticket_Counts>
        // Msg for TickerService
        Message<String> message = MessageBuilder.withPayload("TICKET_SAVE" + ","
                + ticket.getTicketLocation() + ","
                + ticket.getTicketCounts())
                .build();
        source.output().send(message);
    }


    public Collection<Ticket> getTicketListFallBack() {
        List<Ticket> defaultLocations = new ArrayList<>();
        defaultLocations.add(new Ticket("Mumbai", 30));
        defaultLocations.add(new Ticket("Delhi", 30));
        return defaultLocations;

        // here one can implement the user specific cache requierments
    }


    @HystrixCommand(fallbackMethod = "getTicketListFallBack")
    @GetMapping("/tickets")
    public Collection<Ticket> getTicketList() {

        ParameterizedTypeReference<Resources<Ticket>> ptr = new ParameterizedTypeReference<Resources<Ticket>>() {
        };
        ResponseEntity<Resources<Ticket>> responseEntity =
                this.restTemplate.exchange("http://ticket-service/tickets", HttpMethod.GET, null, ptr);
        Collection<Ticket> data = responseEntity.getBody().getContent();
        return data;

    }


    @PostMapping("/dobooking")
    public String addBooking(@RequestBody Reservation r) {
        // Event Name is DOBOOKING
        // Msg format is DOBOOKING,<Reservation_Name>,<Reservation_Location>,<Reservation_Count>
        // Msg for BookingService
        final String transID = UUID.randomUUID().toString();
        Message<String> message = MessageBuilder.withPayload("DOBOOKING" + ","
                + r.getReservationName() + ","
                + r.getReservationLocation() + ","
                + r.getReservationCount() + ","
                + transID)
                .build();
        source.output().send(message);
        return transID;
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

// Data Transfer Object DTO for Reservation
class Reservation {

    private String reservationName;
    private String reservationLocation;
    private int reservationCount;
    private String reservationTransactionID;

    public Reservation() {

    }

    public Reservation(String reservationName, String reservationLocation, int reservationCount, String reservationTransactionID) {
        this.reservationName = reservationName;
        this.reservationLocation = reservationLocation;
        this.reservationCount = reservationCount;
        this.reservationTransactionID = reservationTransactionID;
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
    public String getReservationTransactionID() {
        return reservationTransactionID;
    }
}