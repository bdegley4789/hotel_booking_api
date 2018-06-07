package com.catalyte.training.hotel_room_booking.Controllers;

import com.catalyte.training.hotel_room_booking.CustomExceptions.CustomerAlreadyExists;
import com.catalyte.training.hotel_room_booking.CustomExceptions.CustomerNotFound;
import com.catalyte.training.hotel_room_booking.Entities.Address;
import com.catalyte.training.hotel_room_booking.Entities.Customer;
import com.catalyte.training.hotel_room_booking.Repositories.CustomerRepository;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/customers")
@Api(value="Customer Controller API", produces = "Provides RESTful endpoint for Customers")
public class CustomerController {
    private List<Customer> customers = new ArrayList<Customer>();

    @Autowired
    CustomerRepository customerRepository;

    //Gets all customers, or accepts query statements.
    @RequestMapping(method = RequestMethod.GET)
    @ApiOperation("Get's all customers in the system. If the isActive parameter is set only returns customer that match.")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "OK", responseContainer = "List", response = Customer.class)})
    public List<Customer> getCustomerParam(@RequestParam(required = false) String customerId,
                                           @RequestParam(required = false) String firstName,
                                           @RequestParam(required = false) String lastName,
                                           @RequestParam(required = false) Address address) {
        List<Customer> tempCustomers = new ArrayList<Customer>();
        for (Customer c : customerRepository.findAll()) {
            boolean match = true;
            //User can type any combination of parameters and this will find what customers meet those criteria
            if (customerId != null) {
                if (!c.getCustomerId().equalsIgnoreCase(customerId)) {
                    match = false;
                }
            }
            if (firstName != null) {
                if (!c.getFirstName().equalsIgnoreCase(firstName)) {
                    match = false;
                }
            }
            if (lastName != null) {
                if (!c.getLastName().equalsIgnoreCase(lastName)) {
                    match = false;
                }
            }
            if (address != null) {
                if (c.getAddress() != address) {
                    match = false;
                }
            }
            if (match) {
                tempCustomers.add(c);
            }
        }
        //If no parameters are typed all customers are returned
        if (customerId == null && firstName == null && lastName == null && address == null) {
            return customerRepository.findAll();
            //If parameters are typed and 1 or more is found then they will be all displayed
        } else if (tempCustomers.size() > 0) {
            return tempCustomers;
            //If no customers meeting parameters are found customer not found is thrown
        } else {
            throw new CustomerNotFound();
        }
    }

    //Get customer by Id
    @RequestMapping(value = "/{customerId}", method = RequestMethod.GET)
    @ApiOperation("Gets the customer in the system for the supplied id")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "OK", response = Customer.class)})
    public Customer getCustomer(@PathVariable String customerId) {
        //Finds customer by Id
        for (Customer c : customerRepository.findAll()) {
            if (c.getCustomerId().equals(customerId)) {
                return c;
            }
        }
        throw new CustomerNotFound();
    }

    //Delete Customer
    @RequestMapping(value = "/{customerId}", method = RequestMethod.DELETE)
    @ApiOperation("Deletes the customer in the system for the supplied id")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "OK", response = Customer.class)})
    public void deleteCustomer(@PathVariable String customerId) {
        //Finds and deletes customer by Id
        for (Customer c : customerRepository.findAll()) {
            if (c.getCustomerId().equals(customerId)) {
                customerRepository.delete(c);
                System.out.println("Customer Deleted");
            }
        }
    }

    //Post new customer
    @RequestMapping(method = RequestMethod.POST)
    @ApiOperation("Creates a new customer in the system")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "OK", response = Customer.class)})
    public void addCustomer(@RequestParam (required = false) String firstName,
                            @RequestParam (required = false) String lastName,
                            @RequestParam (required = false) Address address,
                            @RequestBody Customer customer) {
        //If the user types something in the text boxes that takes priority
        //If nothing is types in the text boxes the default ir Request Body
        if (firstName != null) {
            customer.setFirstName(firstName);
        }
        if (lastName != null) {
            customer.setLastName(lastName);
        }
        if (address != null) {
            customer.setAddress(address);
        }
        //New customer is added
        customerRepository.save(customer);
        System.out.println("Customer successfully added.");
    }

    //Update a customer by customerId
    @RequestMapping(method = RequestMethod.PUT)
    @ApiOperation("Updates the customer for the supplied ID")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "OK", response = Customer.class),
            @ApiResponse(code = 404, message = "The Customer was not located within the system", response = CustomerNotFound.class)})
    public void updateUser(@RequestParam (required = false) String customerId,
                           @RequestParam (required = false) String firstName,
                           @RequestParam (required = false) String lastName,
                           @RequestParam (required = false) Address address,
                           @RequestBody Customer customer) {
        //If the user types something in the text boxes that takes priority
        //If nothing is types in the text boxes the default ir Request Body
        if (customerId != null) {
            customer.setCustomerId(customerId);
        }
        if (firstName != null) {
            customer.setFirstName(firstName);
        }
        if (lastName != null) {
            customer.setLastName(lastName);
        }
        if (address != null) {
            customer.setAddress(address);
        }
        //Finds customer by ID updates information
        for (Customer c : customerRepository.findAll()) {
            if (c.getCustomerId().equals(customer.getCustomerId())) {
                c.setFirstName(customer.getFirstName());
                c.setLastName(customer.getLastName());
                c.setAddress(customer.getAddress());
                customerRepository.save(c);
                System.out.println("Customer Updated");
            }
        }
    }
}