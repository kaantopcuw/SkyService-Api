package com.sky.apigateway.controller;

import com.sky.apigateway.model.AvailabilityRequest;
import com.sky.apigateway.model.Flight;
import com.sky.apigateway.model.Port;
import com.sky.apigateway.service.AvailabilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = {"*"}, allowedHeaders = "*", allowCredentials = "true")
public class AvailabilityController {

    private final AvailabilityService avilabilityService;

    @GetMapping("/portGroups")
    public Map<String, List<Port>> portGroups() {
        return avilabilityService.getPortGroups();
    }

    @GetMapping("/portsByCountry/{portCode}")
    public Map<String, List<Port>> portsByCountry(@PathVariable String portCode) {
        if (portCode == null || portCode.isEmpty())
            return null;
        return avilabilityService.getPortsByCountry(portCode);
    }

    @PostMapping("/availability")
    public List<Flight> getAvailability(@RequestBody AvailabilityRequest request) {
        return avilabilityService.getAvailability(request);
    }


}
