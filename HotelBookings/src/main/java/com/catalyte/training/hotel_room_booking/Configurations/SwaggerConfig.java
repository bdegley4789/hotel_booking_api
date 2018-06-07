package com.catalyte.training.hotel_room_booking.Configurations;


import com.catalyte.training.hotel_room_booking.Controllers.ReservationController;
import com.catalyte.training.hotel_room_booking.Entities.Reservation;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;


@EnableSwagger2
@ComponentScan(basePackageClasses = {ReservationController.class})
@Configuration
public class SwaggerConfig {
    private static final String SWAGGER_API_VERSION = "1.0";
    private static final String LICENSE_TEXT = "License";
    private static final String title = "Hotel Room Reservation Calculation " +
            "API";
    private static final String description = "RESTful API For Calculating " +
            "Reservations for Hotel Rooms";

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder().title(title).description(description)
                .license(LICENSE_TEXT).version(SWAGGER_API_VERSION).build();
    }

    @Bean
    public Docket usersAPI() {
        return new Docket(DocumentationType.SWAGGER_2).apiInfo(apiInfo())
                .pathMapping("/").select().apis(RequestHandlerSelectors
                        .basePackage("com.catalyte.training" +
                                ".hotel_room_booking.Controllers")).build();
    }
}