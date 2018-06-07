package com.catalyte.training.hotel_room_booking.Repositories;

import com.catalyte.training.hotel_room_booking.Entities.Customer;
import com.catalyte.training.hotel_room_booking.Entities.Reservation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ReservationRepository extends MongoRepository<Reservation, String> {

  public List<Reservation> findByReservationId(String reservationId);

}
