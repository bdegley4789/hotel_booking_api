package com.catalyte.training.hotel_room_booking.Controllers;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

@RestController
@RequestMapping("/currency")
@Api(value="Currency Controller API", produces = "Provides RESTful endpoint for Currency")
public class CurrencyController {
    //Constant for the URL. If URL is changed this is the only value that needs to be updated
    public static final String apiURL = "http://localhost:8080/currency-exchange";
    @RequestMapping(method = RequestMethod.GET)
    @ApiOperation("Get's all currencies in the system or gets the specified currency if one is supplied.")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "OK", responseContainer = "String")})
    public String getCurrency(@RequestParam(required = false) String currencyName){
        //Use room rates API to get currencies
        String requestUrl= apiURL;
        if (currencyName != null) {
            requestUrl += "?currencyName=" + currencyName;
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
        return jsonString.toString();
    }

    @RequestMapping(method = RequestMethod.POST)
    @ApiOperation("Create New Currency")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "OK", responseContainer = "String")})
    public String createCurrency(@RequestParam(required = true) String currencyName,
                                 @RequestParam(required = true) Double currencyExchangeRate){
        //Use room rates API to create new currency
        String payload="{\"currencyName\": \"" + currencyName + "\",\"currencyExchangeRate\": " + currencyExchangeRate + "}";
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
        return jsonString.toString();
    }

    @RequestMapping(method = RequestMethod.PUT)
    @ApiOperation("Update Currency")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "OK", responseContainer = "String")})
    public String updateCurrency(@RequestParam(required = true) String newCurrencyName,
                                 @RequestParam(required = true) Double newCurrencyExchangeRate,
                                 @RequestParam(required = true) String oldCurrencyName,
                                 @RequestParam(required = true) Double oldCurrencyExchangeRate){
        //Use room rates API to update currency
        String payload = "{\n" +
                "  \"newCurrency\": {\n" +
                "    \"currencyName\": \"" + newCurrencyName + "\",\n" +
                "    \"currencyExchangeRate\": " + newCurrencyExchangeRate + "\n" +
                "  },\n" +
                "  \"oldCurrency\": {\n" +
                "    \"currencyName\": \"" + oldCurrencyName + "\",\n" +
                "    \"currencyExchangeRate\": " + oldCurrencyExchangeRate + "\n" +
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
        return jsonString.toString();
    }

    @RequestMapping(method = RequestMethod.DELETE)
    @ApiOperation("Delete Currency")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "OK", responseContainer = "String")})
    public String deleteCurrency(@RequestParam(required = true) String currencyName){
        //Use room rate API to delete currency
        String requestUrl= apiURL;
        requestUrl += "?currencyName=" + currencyName;
        StringBuffer jsonString = new StringBuffer();
        try {
            URL url = new URL(requestUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setRequestMethod("DELETE");
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
        return jsonString.toString();
    }
}
