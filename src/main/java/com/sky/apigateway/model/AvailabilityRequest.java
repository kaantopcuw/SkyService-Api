package com.sky.apigateway.model;

import jakarta.persistence.Embeddable;
import lombok.Data;

@Data
@Embeddable
public class AvailabilityRequest {
    private String tripType;
    private String depPort;
    private String arrPort;
    private String departureDate;
    private String passengerType;
    private int quantity;
    private String currency;
    private String cabinClass;
    private String lang;
}
