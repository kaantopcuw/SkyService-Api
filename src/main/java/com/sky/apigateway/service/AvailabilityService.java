package com.sky.apigateway.service;

import com.sky.apigateway.model.AvailabilityRequest;
import com.sky.apigateway.model.Flight;
import com.sky.apigateway.model.Port;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private final SkyAlpsService skyAlpsService;
    private final BahamasService bahamasService;
    private final SunriseService sunriseService;

    @Cacheable("portGroupsAvail")
    public Map<String, List<Port>> getPortGroups() {
        Map<String, List<Port>> result = new HashMap<>();

        // Tüm servislerin port gruplarını al
        List<Map<String, List<Port>>> allPortGroups = new ArrayList<>();
        allPortGroups.add(skyAlpsService.getPortGroups());
        allPortGroups.add(bahamasService.getPortGroups());
        allPortGroups.add(sunriseService.getPortGroups());

        // Her servisten gelen port gruplarını işle
        for (Map<String, List<Port>> portGroupMap : allPortGroups) {
            if (portGroupMap != null) {
                for (Map.Entry<String, List<Port>> entry : portGroupMap.entrySet()) {
                    String countryKey = entry.getKey();
                    List<Port> ports = entry.getValue();

                    // Eğer bu ülke zaten result'ta yoksa, yeni liste oluştur
                    if (!result.containsKey(countryKey)) {
                        result.put(countryKey, new ArrayList<>());
                    }

                    // Bu ülkenin mevcut portlarını al
                    List<Port> existingPorts = result.get(countryKey);
                    Set<String> existingCodes = new HashSet<>();

                    // Mevcut port kodlarını topla
                    for (Port existingPort : existingPorts) {
                        existingCodes.add(existingPort.getCode());
                    }

                    // Yeni portları ekle (sadece daha önce eklenmemiş olanları)
                    if (ports != null) {
                        for (Port port : ports) {
                            if (port != null && !existingCodes.contains(port.getCode())) {
                                existingPorts.add(port);
                                existingCodes.add(port.getCode());
                            }
                        }
                    }
                }
            }
        }

        // Her ülkenin portlarını code'a göre sırala
        for (List<Port> ports : result.values()) {
            ports.sort(Comparator.comparing(Port::getCode));
        }

        return result;

    }

    @Cacheable(value = "portsByCountry", key = "#portCode")
    public Map<String, List<Port>> getPortsByCountry(String portCode) {
        return Stream.of(
                        skyAlpsService.getPortsByCountry(portCode),
                        bahamasService.getPortsByCountry(portCode),
                        sunriseService.getPortsByCountry(portCode)
                )
                .flatMap(List::stream)
                .filter(Objects::nonNull)
                .flatMap(map -> map.entrySet().stream())
                .flatMap(entry -> entry.getValue().stream().map(port -> new AbstractMap.SimpleEntry<>(entry.getKey(), port)))
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        Collectors.mapping(
                                Map.Entry::getValue,
                                Collectors.collectingAndThen(
                                        Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(Port::getCode))),
                                        ArrayList::new
                                )
                        )
                ));
    }

    public List<Flight> getAvailability(AvailabilityRequest availabilityRequest) {
        return getAvailability(availabilityRequest.getTripType(), availabilityRequest.getDepPort(), availabilityRequest.getArrPort(), availabilityRequest.getDepartureDate(),
                availabilityRequest.getPassengerType(), availabilityRequest.getQuantity(), availabilityRequest.getCurrency(), availabilityRequest.getCabinClass(), availabilityRequest.getLang());
    }

    public List<Flight> getAvailability(String tripType, String depPort, String arrPort, String departureDate,
                                        String passengerType, int quantity, String currency, String cabinClass, String lang) {
        return Stream.of(
                        skyAlpsService.getAvailability(tripType, depPort, arrPort, departureDate, passengerType, quantity, currency, cabinClass, lang),
                        bahamasService.getAvailability(tripType, depPort, arrPort, departureDate, passengerType, quantity, currency, cabinClass, lang),
                        sunriseService.getAvailability(tripType, depPort, arrPort, departureDate, passengerType, quantity, currency, cabinClass, lang)
                )
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

}
