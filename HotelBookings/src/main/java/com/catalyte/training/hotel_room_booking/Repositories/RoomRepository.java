package com.catalyte.training.hotel_room_booking.Repositories;

import com.catalyte.training.hotel_room_booking.Entities.Room;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface RoomRepository extends MongoRepository<Room, String> {

    public List<Room> findByRoomType(String roomType);

}