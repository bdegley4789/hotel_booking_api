package com.catalyte.training.hotel_room_booking.Controllers;

import com.catalyte.training.hotel_room_booking.CustomExceptions.ReservationNotFound;
import com.catalyte.training.hotel_room_booking.Entities.Reservation;
import com.catalyte.training.hotel_room_booking.Entities.Room;
import com.catalyte.training.hotel_room_booking.Repositories.ReservationRepository;
import com.mongodb.util.JSON;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

@RestController
@RequestMapping("/reservations")
@Api(value="Reservation Controller API", produces = "Provides RESTful endpoint for Reservations")
public class ReservationController {
    private List<Reservation> reservations = new ArrayList<Reservation>();

    @Autowired
    ReservationRepository reservationRepository;

    //Gets all customers, or accepts query statements.
    @RequestMapping(method = RequestMethod.GET)
    @ApiOperation("Get's all reservations in the system. If the isActive parameter is set only returns reservation that matches.")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "OK", responseContainer = "List", response = Reservation.class)})
    public List<Reservation> getReservationParam(@RequestParam(required = false) String reservationId,
                                                 @RequestParam(required = false) String selectedRoom,
                                                 @RequestParam(required = false) String customerId,
                                                 @RequestParam(required = false) Double totalCost,
                                                 @RequestParam(required = false) Double costPerNight,
                                                 @RequestParam(required = false) String startDate,
                                                 @RequestParam(required = false) String endDate,
                                                 @RequestParam(required = false) String selectedCurrency){
        List<Reservation> tempReservations = new ArrayList<Reservation>();
        Date sDate = null;
        Date eDate = null;
        //Change start and end date from string to date
        if (startDate != null) {
            try {
                sDate = new SimpleDateFormat("yyyy-MM-dd").parse(startDate);
            } catch(Exception e) {
            }
        }
        if (endDate != null) {
            try {
                eDate = new SimpleDateFormat("yyyy-MM-dd").parse(endDate);
            } catch(Exception e) {
            }
        }
        for (Reservation c : reservationRepository.findAll()) {
            boolean match = true;
            //The user can type in what ever combination of parameters they want to
            if (reservationId != null) {
                if (!c.getReservationId().equalsIgnoreCase(reservationId)) {
                    match = false;
                }
            }
            if (totalCost != null) {
                if (Double.compare(c.getTotalCost(),totalCost) != 0) {
                    match = false;
                }
            }
            if (customerId != null) {
                if (!c.getCustomerId().equalsIgnoreCase(customerId)) {
                    match = false;
                }
            }
            if (costPerNight != null) {
                if (Double.compare(c.getCostPerNight(),costPerNight) != 0) {
                    match = false;
                }
            }
            if (sDate != null) {
                if (c.getStartDate().after(sDate) || c.getStartDate().before(sDate)) {
                    match = false;
                }
            }
            if (eDate != null) {
                if (c.getEndDate().compareTo(eDate) != 0) {
                    match = false;
                }
            }
            if (selectedCurrency != null) {
                if (!c.getSelectedCurrency().equalsIgnoreCase(selectedCurrency)) {
                    match = false;
                }
            }
            if (selectedRoom != null) {
                if (!c.getSelectedRoom().equalsIgnoreCase(selectedRoom)) {
                    match = false;
                }
            }
            if (match) {
                tempReservations.add(c);
            }
        }
        //If no paramters are entered return everything
        if (selectedRoom == null && customerId == null && totalCost == null && costPerNight == null && startDate == null && endDate == null && selectedCurrency == null) {
            return reservationRepository.findAll();
            //If parameters are typed in then return all reservations that match
        } else if (tempReservations.size() > 0) {
            return tempReservations;
            //Throw error if reservations are found
        } else {
            throw new ReservationNotFound();
        }
    }

    //Get reservation by Id
    @RequestMapping(value = "/{reservationId}", method = RequestMethod.GET)
    @ApiOperation("Gets the reservation in the system for the supplied id")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "OK", response = Reservation.class)})
    public Reservation getReservation(@PathVariable String reservationId) {
        for (Reservation c : reservationRepository.findAll()) {
            if (c.getReservationId().equals(reservationId)) {
                return c;
            }
        }
        throw new ReservationNotFound();
    }

    //Delete Reservation
    @RequestMapping(value = "/{reservationId}", method = RequestMethod.DELETE)
    @ApiOperation("Deletes the reservation in the system for the supplied id")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "OK", response = Reservation.class)})
    public void deleteReservation(@PathVariable String reservationId) {
        for (Reservation c : reservationRepository.findAll()) {
            if (c.getReservationId().equals(reservationId)) {
                RoomController roomController = new RoomController();
                //Increment rooms in use by 1
                updateRoomsInUse(c.getSelectedRoom(),-1);
                reservationRepository.delete(c);
                System.out.println("Reservation Deleted");
            }
        }
    }

    //Post new reservation
    @RequestMapping(method = RequestMethod.POST)
    @ApiOperation("Creates a new reservation in the system")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "OK", response = Reservation.class)})
    public void addReservation(@RequestBody Reservation reservation) {
        //Send request for daily cost and total cost of room
        double [] arr = sendPostRequest(reservation.getEndDate(),reservation.getStartDate(),reservation.getSelectedRoom(),reservation.getSelectedCurrency());
        //Set new costs
        reservation.setCostPerNight(arr[1]);
        reservation.setTotalCost(arr[0]);
        //Change date to PST time zone
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        String start = format.format(reservation.getStartDate());
        String end = format.format(reservation.getEndDate());
        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd");
        isoFormat.setTimeZone(TimeZone.getTimeZone("PST"));
        //Increment date by 1 day. For some reason setting timezone to PST sets date back one day
        try {
            Calendar c = Calendar.getInstance();
            c.setTime(isoFormat.parse(start));
            c.add(Calendar.DATE, 1);
            //Increment Start Date by 1
            start = isoFormat.format(c.getTime());
            reservation.setStartDate(isoFormat.parse(start));
        } catch (Exception e) {
        }
        try {
            Calendar c = Calendar.getInstance();
            c.setTime(isoFormat.parse(end));
            c.add(Calendar.DATE, 1);
            //Increment End Date by 1
            end = isoFormat.format(c.getTime());
            reservation.setEndDate(isoFormat.parse(end));
        } catch (Exception e) {
        }
        //Increment rooms in use by 1
        updateRoomsInUse(reservation.getSelectedRoom(),1);
        reservationRepository.save(reservation);
        System.out.println("Reservation successfully added.");
    }

    //Updates a reservation by reservationId
    @RequestMapping(method = RequestMethod.PUT)
    @ApiOperation("Updates the reservation for the supplied ID")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "OK", response = Reservation.class),
            @ApiResponse(code = 404, message = "The Reservation was not located within the system", response = ReservationNotFound.class)})
    public void updateReservation(@RequestBody Reservation reservation) {
        //Find reservation by reservation ID
        for (Reservation c : reservationRepository.findAll()) {
            if (c.getReservationId().equals(reservation.getReservationId())) {
                RoomController roomController = new RoomController();
                //Decrement old room type in use by 1
                updateRoomsInUse(c.getSelectedRoom(),-1);
                //Increment new room type in use by 1
                updateRoomsInUse(reservation.getSelectedRoom(),1);
                //Update Reservation information
                c.setCustomerId(reservation.getCustomerId());
                c.setSelectedCurrency(reservation.getSelectedCurrency());
                c.setStartDate(reservation.getStartDate());
                c.setEndDate(reservation.getEndDate());
                c.setSelectedRoom(reservation.getSelectedRoom());
                //Calculate and set new room cost
                double [] arr = sendPostRequest(reservation.getEndDate(),reservation.getStartDate(),reservation.getSelectedRoom(),reservation.getSelectedCurrency());
                c.setCostPerNight(arr[1]);
                c.setTotalCost(arr[0]);
                //Save and Update reservation
                reservationRepository.save(c);
                System.out.println("Reservation Updated");
            }
        }
    }
    //Adapted From: https://stackoverflow.com/questions/15570656/how-to-send-request-payload-to-rest-api-in-java
    //This method gets the total cost and daily cost of a room based on room type, currency and dates
    public static double[] sendPostRequest(Date endDate, Date startDate, String roomType, String currency) {
        Date todaysDate = new Date();
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        String eDate = df.format(endDate);
        String sDate = df.format(startDate);
        //escape the double quotes in json string
        String payload="{\"endDate\":\"" + eDate + "\",\"roomType\":\"" + roomType + "\",\"selectedCurrency\":\"" + currency + "\",\"startDate\":\"" + sDate + "\"}";
        String requestUrl="http://localhost:8080/RoomCalculator";
        StringBuffer jsonString = new StringBuffer();
        double[] cost = new double[2];
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
            String jString = jsonString.toString();
            JSONObject jsonObj = new JSONObject(jString);
            System.out.print("TotalCost: " + jsonObj.get("totalCost"));
            System.out.println(" costPerNight: "+ jsonObj.get("costPerNight"));
            cost[0] = new Double(jsonObj.get("totalCost").toString());
            cost[1] = new Double(jsonObj.get("costPerNight").toString());
            connection.disconnect();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
        return cost;
    }

    //This method is used to call my RoomController and update the count of rooms in use
    //The value supplied will either by 1 or -1 for increment or decrement
    public void updateRoomsInUse(String roomType, int value) {
        roomType = roomType.replaceAll(" ", "%20");
        String requestUrl = "http://localhost:8090/Rooms/" + roomType + "?value=" + value;
        StringBuffer jsonString = new StringBuffer();
        try {
            URL url = new URL(requestUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setRequestMethod("PUT");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.disconnect();
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
    }
}