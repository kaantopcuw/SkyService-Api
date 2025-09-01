package com.sky.apigateway.service;

import com.sky.apigateway.model.Fare;
import com.sky.apigateway.model.Flight;
import com.sky.apigateway.model.Port;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.sky.apigateway.utils.ServiceUtils.parsePrice;

@Service
public class SunriseService {

    private final WebClient webClient;

    public SunriseService(WebClient.Builder webClientBuilder) {
        ConnectionProvider provider = ConnectionProvider.builder("sunrise-connection-provider")
                .maxIdleTime(Duration.ofSeconds(30)) // Boşta kalan bağlantıları 30 saniye sonra kapat
                .build();


        this.webClient = webClientBuilder
                .clientConnector(new ReactorClientHttpConnector(HttpClient.create(provider)))
                .baseUrl("https://book.sunriseairways.net/ibe").build();
    }


    @Cacheable("portGroupsSunrise")
    public Map<String, List<Port>> getPortGroups() {
        return webClient.get()
                .uri("/search/portGroups")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, List<Port>>>() {
                })
                .block();
    }


    @Cacheable(value = "portsByCountrySunrise", key = "#portCode")
    public List<Map<String, List<Port>>> getPortsByCountry(String portCode) {
        return webClient.get()
                .uri("/search/portGroupsByCountry/{portCode}", portCode)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Map<String, List<Port>>>>() {
                })
                .block();
    }

    public List<Flight> getAvailability(String tripType, String depPort, String arrPort, String departureDate,
                                        String passengerType, int quantity, String currency, String cabinClass, String lang) {
        String htmlResponse = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/availability")
                        .queryParam("tripType", tripType)
                        .queryParam("depPort", depPort)
                        .queryParam("arrPort", arrPort)
                        .queryParam("departureDate", departureDate)
                        .queryParam("passengerQuantities[0][passengerType]", passengerType)
                        .queryParam("passengerQuantities[0][quantity]", quantity)
                        .queryParam("currency", currency)
                        .queryParam("cabinClass", cabinClass)
                        .queryParam("lang", lang)
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .block();

        List<Flight> flightList = new ArrayList<>();
        if (htmlResponse != null) {
            Document doc = Jsoup.parse(htmlResponse);
            Elements journeyElements = doc.select("#availability-flight-table-0 .js-journey");

            for (Element journeyElement : journeyElements) {
                Element infoRow = journeyElement.selectFirst(".info-row");
                if (infoRow == null) continue;

                String departureTime = infoRow.select(".info-block").get(0).select(".time").text();
                String departurePort = infoRow.select(".info-block").get(0).select(".port").text();
                String departureDateText = infoRow.select(".info-block").get(0).select(".date").text();

                String arrivalTime = infoRow.select(".info-block.text-right").get(0).select(".time").text();
                String arrivalPort = infoRow.select(".info-block.text-right").get(0).select(".port").text();
                String arrivalDateText = infoRow.select(".info-block.text-right").get(0).select(".date").text();

                String duration = infoRow.selectFirst(".middle-block .flight-duration").text();
                String flightNumber = infoRow.selectFirst(".middle-block .flight-no").text();
                String totalStop = infoRow.selectFirst(".middle-block .total-stop").text();

                List<Fare> fares = new ArrayList<>();
                Elements fareElements = journeyElement.select(".flight-table__flight-type-column");
                for (Element fareElement : fareElements) {
                    String fareType = fareElement.select(".flight-table__flight-type-column-title").text();
                    String priceText = fareElement.select(".button").text();
                    double price = parsePrice(priceText);

                    List<String> details = new ArrayList<>();
                    Elements detailsElements = fareElement.select(".branded-fare-content-label");
                    for (Element detailElement : detailsElements) {
                        details.add(detailElement.text());
                    }

                    fares.add(Fare.builder().fareType(fareType).price(price).details(details).build());
                }

                Flight flight = Flight.builder()
                        .departureTime(departureTime)
                        .departurePort(departurePort)
                        .departureDate(departureDateText)
                        .arrivalTime(arrivalTime)
                        .arrivalPort(arrivalPort)
                        .arrivalDate(arrivalDateText)
                        .duration(duration)
                        .flightNumber(flightNumber)
                        .totalStop(totalStop)
                        .fares(fares)
                        .provider("Sunrise Air")
                        .build();
                flightList.add(flight);
            }
        }
        return flightList;
    }
}
