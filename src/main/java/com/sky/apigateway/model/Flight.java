package com.sky.apigateway.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class Flight {
    private String departureTime;
    private String departurePort;
    private String departureDate;
    private String arrivalTime;
    private String arrivalPort;
    private String arrivalDate;
    private String duration;
    private String flightNumber;
    private String totalStop;
    private List<Fare> fares;
    private String provider;

    public Double getMinFarePrice() {
        return fares.stream()
            .filter(fare -> fare.getPrice() != null)
            .mapToDouble(Fare::getPrice)
            .min()
            .orElse(Double.MAX_VALUE);
    }
}
