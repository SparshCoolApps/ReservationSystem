# ReservationSystem

ReservationSystem basically demonstrate the SpringBoot Microservices capabilities. This demo has following compononts:

1. Configuration Server - It uses the spring cloud config server component to create a configuration server which keeps and manage the properties for all the microsevices.

2. Eureka Server - this component implements the Service Registry and client side load balancing. 

3. Ticket Service - This mS is storing the Ticket details like Location and Tickets Count. Initially when service starts up, it just inserts few records for Ticket Location and Counts into in-memory database H2.

4. Reservation Service - This mS stores the reservation records for some random users initially at start-up. Later it can store the acutal records of users. For demo purpose it just maintain all the records into in-memory database H2.

5. Booking Service - this service implement the booking service in reservation system. It uses events to complete the transaction which 
  
    5.1 first create a record for booking into it own in-memory database with a transaction ID 
    
    5.2 second, inserts a record for reservation via reservation-service and
    
    5.3 then deduct the tickets counts of reservation for that location into Tickets master db (in H2) via Ticket service.
    
    
6. ReservationClient (API Gateway) - This service behave as API Gateway and provide the following facilities:

  6.1 List of reservations 
  
  6.2 Adding only reservation record directly to Reservation Master via reservation-service 
  
  6.3 List of Tickets 
  
  6.4 Adding only ticket record directly to Ticket Master via ticket-service
  
  6.5 create a booking via Choreography of services like Booking, Reservation and Ticket
  
  
7. Hystrix Dashbord - this component is use to see the visibility of various services 

8. ConfigDir - has all the configuration properties 
  
