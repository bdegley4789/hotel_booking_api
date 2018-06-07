package com.catalyte.training.hotel_room_booking.Repositories;

import com.catalyte.training.hotel_room_booking.Entities.Customer;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface CustomerRepository extends MongoRepository<Customer, String> {

  public List<Customer> findByFirstName(String firstName);

  public List<Customer> findByCustomerId(String customerId);

}
