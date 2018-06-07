Bryce Egley Hotel Bookings

# Set Up

On http://localhost:8080 Run hotel-room-rates-api

In HotelBookings IntelliJ Run -> Edit Configurations -> Set VM Options to "-Dserver.port=8090"
Run HotelBookings on http://localhost:8090

*Note You need to use these exact port numbers or the api will not work*

# Unit Testing

Go to src -> test -> java -> com.catalyte.training.hotel_room_booking.Controllers
Right Click on directory and select Run Tests with Coverage

Controllers have 62% line coverage
Unit Tests were generated with [EvoSuite](http://www.evosuite.org/)

*I installed evosuite to IntelliJ. If there are compiler errors caused by evosuite on your machine remove the .evosuite directory*

# HotelBookings API Tutorial
*This tutorial will cover basic functionality of my API*

## Room
#### Get
Go to http://localhost:8090/swagger-ui.html#/room-controller/getRoomsUsingGET
1. Get - Execute Get to see all the current rooms
2. Type in "Queen Double" in the Get box to see the information for just that room
*Execute Get Room requests after each of the following to observe changes*

#### Post
Go to http://localhost:8090/swagger-ui.html#/room-controller/createRoomTypeUsingPOST
3. Type in "New Suite" for Room Type, 50 for Room Rate, 5 for Room Count
4. Click execute
5. Go back to Get Rooms and click execute to see the new room type you created

#### Put
Go to http://localhost:8090/swagger-ui.html#/room-controller/updateCurrencyUsingPUT_1
6. Type in "Newer Suite" for newRoomTypeName, 60 for newRoomRate, 6 for roomCount,
"New Suite" for oldRoomTypeName, 50 for oldRoomRate
7. Click execute
8. Go back to Get Rooms and click execute to see the room you updated

#### Delete
Go to http://localhost:8090/swagger-ui.html#/room-controller/deleteCurrencyUsingDELETE_1
9. Type in "Newer Suite" for roomTypeName, 60 for roomRate
10. Click execute
11. Go back to Get Rooms and click execute to see that the room is deleted


## Customer
#### Get
Go to http://localhost:8090/swagger-ui.html#/customer-controller/getCustomerParamUsingGET
1. Get - Execute Get to see all the current customers
*Execute Get Customers requests after each of the following to observe changes*

#### Post
Go to http://localhost:8090/swagger-ui.html#/customer-controller/addCustomerUsingPOST
2. Put this customer into the body and execute
{
  "address": {
    "city": "Beaverton",
    "state": "Or",
    "street": "111 South",
    "zip": "11111"
  },
  "customerId": "jgksl432",
  "firstName": "John",
  "lastName": "Smith"
}
3. Go back to Get Customers and click execute to see that the customer is added

#### Put
Go to http://localhost:8090/swagger-ui.html#/customer-controller/updateUserUsingPUT
4. Put this customer into the body and execute
{
  "address": {
    "city": "Portland",
    "state": "Or",
    "street": "111 South",
    "zip": "11111"
  },
  "customerId": "jgksl432",
  "firstName": "Bob",
  "lastName": "Smith"
}
5. Go back to Get Customers and click execute to see that the customers first name and city were changed

#### Get by CustomerId
Go to http://localhost:8090/swagger-ui.html#/customer-controller/getCustomerUsingGET
6. Enter the id "jgksl432" into the id box
7. Click execute and observe the information displayed for our customer

#### Delete
Go to http://localhost:8090/swagger-ui.html#/customer-controller/deleteCustomerUsingDELETE
8. Enter the id "jgksl432" into the id box
9. Click execute
10. Go back to Get /customers and execute. Observe our customer is gone


## Currency
#### Get
Go to http://localhost:8090/swagger-ui.html#/currency-controller/getCurrencyUsingGET
1. Get - Execute Get to see all the current currencies
*Execute Get Currency requests after each of the following to observe changes*

#### Post
Go to http://localhost:8090/swagger-ui.html#/currency-controller/createCurrencyUsingPOST
2. For currency name type "ETH" for currencyExchangeRate type 1000
3. Click execute
4. Go back to Get and see the new currency we created

#### Put
Go to http://localhost:8090/swagger-ui.html#/currency-controller/updateCurrencyUsingPUT
6. For newCurrencyName type "ETHER", for newCurrencyExchangeRate type 2000,
for oldCurrencyName type "ETH", for oldCurrencyExchangeRate type 1000
7. Click execute
8. Go back to Get and see how the currency updated

#### Delete
Go to http://localhost:8090/swagger-ui.html#/currency-controller/deleteCurrencyUsingDELETE
9. For currencyName type "ETHER"
10. Click execute
11. Go back to Get and see how the currency is now gone


## Reservations
#### Get
Go to http://localhost:8090/swagger-ui.html#/reservation-controller/getReservationParamUsingGET
1. Get - Execute Get to see all the current reservations
*Execute Get Reservations requests after each of the following to observe reservation changes*
*Execute Get Room requests after each of the following to observe room changes*

#### Post
Go to http://localhost:8090/swagger-ui.html#/reservation-controller/addReservationUsingPOST
2. Put the following reservation into the body
{
    "reservationId": "fhjdjuchdjdhjd",
    "customerId": "kiehdnsmsk",
    "selectedRoom": "Honeymoon Suite",
    "totalCost": 0,
    "costPerNight": 0,
    "startDate": "2018-06-03",
    "endDate": "2018-06-06",
    "selectedCurrency": "USD"
  }
3. Click execute
4. Go to Get reservations and view the new reservation created. Observe that cost has been done automatically.
5. Go to Get Rooms and view the rooms in use for Honeymoon Suite has been incremented

#### Put
Go to http://localhost:8090/swagger-ui.html#/reservation-controller/updateReservationUsingPUT
6. Put the following updated reservation into the body
{
    "reservationId": "fhjdjuchdjdhjd",
    "customerId": "kiehdnsmsk",
    "selectedRoom": "Presidential Suite",
    "totalCost": 0,
    "costPerNight": 0,
    "startDate": "2018-06-03",
    "endDate": "2018-06-07",
    "selectedCurrency": "USD"
  }
7. Go to Get Reservations and view the updated reservation. Observe the cost has changed automatically
8. Go to Get Rooms and view the rooms in use for Honeymoon Suite had been decremented and Presidential Suite has been incremented

#### Delete
Go to http://localhost:8090/swagger-ui.html#/reservation-controller/deleteReservationUsingDELETE
9. Put our reservationId "fhjdjuchdjdhjd" into the reservationId box
10. Click execute
11. Go to Get Reservations and view that are reservation has been deleted
12. Go to Get Rooms and view that Presidential Suite has been decremented

You have now seen the basic functionality of my HotelBookings API!
