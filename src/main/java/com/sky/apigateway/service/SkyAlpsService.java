package com.sky.apigateway.service;

import com.sky.apigateway.model.Fare;
import com.sky.apigateway.model.Flight;
import com.sky.apigateway.model.Port;
import com.sky.apigateway.utils.ServiceUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.sky.apigateway.utils.ServiceUtils.parsePrice;

@Service
public class SkyAlpsService {

    private final WebClient webClient;

    public SkyAlpsService(WebClient.Builder webClientBuilder) {
        ConnectionProvider provider = ConnectionProvider.builder("skyalps-connection-provider")
                .maxIdleTime(Duration.ofSeconds(30)) // Boşta kalan bağlantıları 30 saniye sonra kapat
                .build();

        this.webClient = webClientBuilder
                .clientConnector(new ReactorClientHttpConnector(HttpClient.create(provider)))
                .baseUrl("https://book-skyalps.crane.aero/ibe").build();
    }


    @Cacheable("portGroupsSkyAlps")
    public Map<String, List<Port>> getPortGroups() {
        return webClient.get()
                .uri("/search/portGroups")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, List<Port>>>() {
                })
                .block();
    }


    @Cacheable(value = "portsByCountrySkyAlps", key = "#portCode")
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
        ResponseEntity<String> responseEntity
                = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/availability")
                        .queryParam("tripType", tripType)
                        .queryParam("depPort", depPort)
                        .queryParam("arrPort", arrPort)
                        .queryParam("departureDate", ServiceUtils.getUrlDateFormat(departureDate))
                        .queryParam("passengerQuantities[0][passengerType]", passengerType)
                        .queryParam("passengerQuantities[0][quantity]", quantity)
                        .queryParam("currency", currency)
                        .queryParam("cabinClass", cabinClass)
                        .queryParam("lang", lang)
                        .build())
                .retrieve()
                .toEntity(String.class)
                .block();

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        List<Flight> flightList = new ArrayList<>();
        String htmlResponse = null;
        String cookie = null;

        if (responseEntity != null) {
            htmlResponse = responseEntity.getBody();
            cookie = responseEntity.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        }

        if (htmlResponse != null) {
            Document doc = Jsoup.parse(htmlResponse);
            Elements journeyElements = doc.select("#availability-flight-table-0 .js-journey");

            String cid = doc.body().attr("data-cid");

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
                Element fareItemSelector = journeyElement.selectFirst(".js-fare-item-selector");

                if (fareItemSelector != null) {
                    final String fareId = fareItemSelector.attr("data-fare-id");
                    final String journeyType = fareItemSelector.attr("data-journeytype");
                    final String finalJourneyIndex = fareItemSelector.attr("data-index");
                    final String finalCid = cid;


                    String packageListHtml = webClient.get()
                            .uri(uriBuilder -> uriBuilder
                                    .path("/availability/getPackageList")
                                    .queryParam("_cid", finalCid)
                                    .queryParam("fareId", fareId)
                                    .queryParam("availIndex", 0)
                                    .queryParam("journeyIndex", finalJourneyIndex)
                                    .queryParam("journeyType", journeyType)
                                    .build())
                            .header(HttpHeaders.COOKIE, cookie)
                            .retrieve()
                            .bodyToMono(String.class)
                            .block();

                    if (packageListHtml != null) {
                        Document packageDoc = Jsoup.parse(packageListHtml);
                        Elements fareWrappers = packageDoc.select(".js-flight-table-item");
                        for (Element fareWrapper : fareWrappers) {
                            String fareTitle = fareWrapper.selectFirst(".bundle-title").text();
                            String priceText = fareWrapper.selectFirst(".bundle-budget > strong").text();
                            double price = parsePrice(priceText);


                            List<String> details = new ArrayList<>();
                            Elements detailsElements = fareWrapper.select(".bundle-content-text");
                            for (Element detailElement : detailsElements) {
                                details.add(detailElement.text());
                            }

                            fares.add(Fare.builder().fareType(fareTitle).price(price).details(details).build());
                        }
                    }
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
                        .provider("Sky Alps")
                        .build();
                flightList.add(flight);
            }
        }
        return flightList;
    }



}
