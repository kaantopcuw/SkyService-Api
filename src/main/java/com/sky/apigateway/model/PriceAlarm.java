package com.sky.apigateway.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class PriceAlarm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private int userId;
    @Embedded
    private AvailabilityRequest availabilityRequest;
    private double expectedPrice;
    private double lastPrice;
}
