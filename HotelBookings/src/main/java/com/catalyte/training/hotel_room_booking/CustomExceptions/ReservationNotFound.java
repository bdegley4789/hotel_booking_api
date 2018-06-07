package com.catalyte.training.hotel_room_booking.CustomExceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

//404
@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "404 ERROR: The reservation was not located within the system!")
public class ReservationNotFound extends RuntimeException {
}