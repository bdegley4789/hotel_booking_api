package com.catalyte.training.hotel_room_booking.Entities;

import org.springframework.data.annotation.Id;

public class Room {
    @Id
    private String roomId;
    private String roomType;
    private Double roomRate;
    private Integer roomCount;
    private Integer roomUsed;

    @Override
    public String toString() {
        return "Room{" +
                "roomType='" + roomType + '\'' +
                ", roomRate=" + roomRate +
                ", roomCount=" + roomCount +
                ", roomUsed=" + roomUsed +
                '}';
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getRoomType() {
        return roomType;
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
    }

    public Double getRoomRate() {
        return roomRate;
    }

    public void setRoomRate(Double roomRate) {
        this.roomRate = roomRate;
    }

    public Integer getRoomCount() {
        return roomCount;
    }

    public void setRoomCount(Integer roomCount) {
        this.roomCount = roomCount;
    }

    public Integer getRoomUsed() {
        return roomUsed;
    }

    public void setRoomUsed(Integer roomUsed) {
        this.roomUsed = roomUsed;
    }
}
