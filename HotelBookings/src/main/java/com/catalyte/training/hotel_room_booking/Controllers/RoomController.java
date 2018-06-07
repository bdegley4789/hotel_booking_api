package com.catalyte.training.hotel_room_booking.Controllers;

import com.catalyte.training.hotel_room_booking.CustomExceptions.ReservationNotFound;
import com.catalyte.training.hotel_room_booking.CustomExceptions.RoomNotFound;
import com.catalyte.training.hotel_room_booking.CustomExceptions.RoomTypeFull;
import com.catalyte.training.hotel_room_booking.Entities.Reservation;
import com.catalyte.training.hotel_room_booking.Entities.Room;
import com.catalyte.training.hotel_room_booking.Repositories.ReservationRepository;
import com.catalyte.training.hotel_room_booking.Repositories.RoomRepository;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/Rooms")
@Api(value="Room Controller API", produces = "Provides RESTful endpoint for Rooms")
public class RoomController {
    //Constant for the URL. If URL is changed this is the only value that needs to be updated
    public static final String apiURL = "http://localhost:8080/Rooms";

    private List<Room> rooms = new ArrayList<Room>();

    @Autowired
    RoomRepository roomRepository;
    //I would rather get the initial values from room rates api
    //I spent an hour trying to do this but kept having errors with JSON parsing so I just used this method instead.
    public void start() {
        addRoom("Queen Double", 175.0);
        addRoom("Honeymoon Suite", 450.0);
        addRoom("Presidential Suite", 650.0);
        addRoom("Single King", 225.0);
    }

    public void addRoom(String roomType, Double roomRate) {
        Room room = new Room();
        room.setRoomType(roomType);
        room.setRoomRate(roomRate);
        room.setRoomCount(2);
        room.setRoomUsed(0);
        roomRepository.save(room);
    }

    @RequestMapping(method = RequestMethod.GET)
    @ApiOperation("Get's all currencies in the system or gets the specified currency if one is supplied.")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "OK", responseContainer = "String")})
    public List<Room> getRooms(@RequestParam(required = false) String roomName){
        //Load initial data if it isn't already there. Room rates doesn't allow for deleting of these rooms.
        //So I believe this way is fine so long as it is called before any reservations using these rooms.
        if (roomRepository.count() < 4) {
            start();
        }
        //Send API request to room rates to get all specified room types
        String requestUrl= apiURL;
        if (roomName != null) {
            roomName = roomName.replaceAll(" ", "%20");
            requestUrl += "?roomType=" + roomName;
        }
        StringBuffer jsonString = new StringBuffer();
        try {
            URL url = new URL(requestUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            while ((line = br.readLine()) != null) {
                jsonString.append(line);
            }
            br.close();
            connection.disconnect();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
        //Find Room in room repository
        List<Room> tempRooms = new ArrayList<Room>();
        if (roomName != null) {
            roomName = roomName.replaceAll("%20", " ");
        }
        for (Room room : roomRepository.findAll()) {
            boolean match = true;
            if (roomName != null) {
                if (!room.getRoomType().equalsIgnoreCase(roomName)) {
                    match = false;
                }
            }
            if (match) {
                tempRooms.add(room);
            }
        }
        if (roomName == null) {
            return roomRepository.findAll();
        } else if (tempRooms.size() > 0) {
            return tempRooms;
        } else {
            throw new RoomNotFound();
        }
    }

    @RequestMapping(method = RequestMethod.POST)
    @ApiOperation("Create New RoomType")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "OK", responseContainer = "String")})
    public String createRoomType(@RequestParam(required = true) String roomType,
                                 @RequestParam(required = true) Double roomRate,
                                 @RequestParam(required = true) Integer roomCount){
        //Send API request to room rates api to create a new room type
        String payload="{\n" +
                "  \"roomType\": \"" + roomType + "\",\n" +
                "  \"roomRate\": " + roomRate + "\n" +
                "}";
        String requestUrl= apiURL;
        StringBuffer jsonString = new StringBuffer();
        try {
            URL url = new URL(requestUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), "UTF-8");
            writer.write(payload);
            writer.close();
            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            while ((line = br.readLine()) != null) {
                jsonString.append(line);
            }
            br.close();
            connection.disconnect();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
        //Create a new room type in my mongodb and room entity
        Room room = new Room();
        room.setRoomType(roomType);
        room.setRoomRate(roomRate);
        room.setRoomCount(roomCount);
        //Rooms in use is controlled by reservations, initial value is 0
        room.setRoomUsed(0);
        roomRepository.save(room);
        return jsonString.toString();
    }

    @RequestMapping(method = RequestMethod.PUT)
    @ApiOperation("Update Room Type")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "OK", responseContainer = "String")})
    public String updateRoom(@RequestParam(required = true) String newRoomTypeName,
                                 @RequestParam(required = true) Double newRoomRate,
                                 @RequestParam(required = false) Integer roomCount,
                                 @RequestParam(required = true) String oldRoomTypeName,
                                 @RequestParam(required = true) Double oldRoomRate){
        //This method updates a room type
        //The following code sends an API request to room rates api
        String payload = "{\n" +
                "  \"newRoom\": {\n" +
                "    \"roomType\": \"" + newRoomTypeName + "\",\n" +
                "    \"roomRate\": " + newRoomRate + "\n" +
                "  },\n" +
                "  \"oldRoom\": {\n" +
                "    \"roomType\": \"" + oldRoomTypeName + "\",\n" +
                "    \"roomRate\": " + oldRoomRate + "\n" +
                "  }\n" +
                "}";
        String requestUrl= apiURL;
        StringBuffer jsonString = new StringBuffer();
        try {
            URL url = new URL(requestUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setRequestMethod("PUT");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), "UTF-8");
            writer.write(payload);
            writer.close();
            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            while ((line = br.readLine()) != null) {
                jsonString.append(line);
            }
            br.close();
            connection.disconnect();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
        //Within my room entity I update the room information
        for (Room room : roomRepository.findAll()) {
            if (room.getRoomType().equalsIgnoreCase(oldRoomTypeName)) {
                room.setRoomType(newRoomTypeName);
                room.setRoomRate(newRoomRate);
                if (roomCount != null) {
                    room.setRoomCount(roomCount);
                }
                roomRepository.save(room);
                return room.toString();
            }
        }
        return jsonString.toString();
    }

    //Get reservation by Id
    @RequestMapping(value = "/{roomType}", method = RequestMethod.PUT)
    @ApiOperation("increments or decrements the used rooms by room type")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "OK", response = Room.class)})
    public void changeInUse(@PathVariable String roomType,
                                      @RequestParam (required = true) int value) {
        for (Room room : roomRepository.findAll()) {
            if (room.getRoomType().equals(roomType)) {
                //Increment or decrement room type in use value
                room.setRoomUsed(room.getRoomUsed() + value);
                //Some of the rooms were created before this method so this is needed
                if (room.getRoomUsed() < 0) {
                    room.setRoomUsed(0);
                }
                //Throw an error before saving when there are no available rooms
                if (room.getRoomUsed() > room.getRoomCount()) {
                    throw new RoomTypeFull();
                }
                roomRepository.save(room);
                System.out.println("Room In Use Count Updated.");
            }
        }
    }

    @RequestMapping(method = RequestMethod.DELETE)
    @ApiOperation("Delete Room Type")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "OK", responseContainer = "String")})
    public String deleteRoom(@RequestParam(required = true) String roomTypeName,
                                 @RequestParam(required = true) Double roomRate){
        //Finds room to delete based on room name
        for (Room room : roomRepository.findAll()) {
            if (room.getRoomType().equalsIgnoreCase(roomTypeName)) {
                roomRepository.delete(room);
            }
        }
        //Send a request to the room rate api to delete the room type
        String requestUrl= apiURL;
        String payload = "{\n" +
                "  \"roomType\": \"" + roomTypeName + "\",\n" +
                "  \"roomRate\": " + roomRate + "\n" +
                "}";
        StringBuffer jsonString = new StringBuffer();
        try {
            URL url = new URL(requestUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setRequestMethod("DELETE");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), "UTF-8");
            writer.write(payload);
            writer.close();
            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            while ((line = br.readLine()) != null) {
                jsonString.append(line);
            }
            br.close();
            connection.disconnect();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
        return jsonString.toString();
    }
}
