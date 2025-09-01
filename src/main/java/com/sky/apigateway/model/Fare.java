package com.sky.apigateway.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class Fare {
    private String fareType;
    private Double price;
    private List<String> details;
}
