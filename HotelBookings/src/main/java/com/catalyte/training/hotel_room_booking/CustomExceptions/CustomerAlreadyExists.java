package com.catalyte.training.hotel_room_booking.CustomExceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

//409
@ResponseStatus(value = HttpStatus.CONFLICT, reason = "409 ERROR: This customer already exists!")
public class CustomerAlreadyExists extends RuntimeException {
}
