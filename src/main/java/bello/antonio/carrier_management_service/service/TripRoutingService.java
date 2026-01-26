package bello.antonio.carrier_management_service.service;

import bello.antonio.carrier_management_service.dto.RouteInfoDTO;
import com.google.maps.model.LatLng;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TripRoutingService {

    @Value("${google.maps.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    // Geocoding con Google Maps API
    public LatLng geocode(String address) {
        try {
            String url = String.format(
                    "https://maps.googleapis.com/maps/api/geocode/json?address=%s&key=%s",
                    UriUtils.encode(address, StandardCharsets.UTF_8),
                    apiKey
            );

            Map response = restTemplate.getForObject(url, Map.class);
            List results = (List) response.get("results");
            if (results.isEmpty()) {
                throw new RuntimeException("Address not found: " + address);
            }

            Map location = (Map)((Map) results.get(0)).get("geometry");
            Map latLng = (Map) location.get("location");

            double lat = ((Number) latLng.get("lat")).doubleValue();
            double lng = ((Number) latLng.get("lng")).doubleValue();

            return new LatLng(lat, lng);

        } catch (Exception e) {
            throw new RuntimeException("Geocoding failed: " + e.getMessage());
        }
    }

    // Calcolo polyline + distanza
    public RouteInfoDTO computeRoute(LatLng departure, LatLng arrival) {
        String url = "https://routes.googleapis.com/directions/v2:computeRoutes";
        System.out.println("Departure: lat=" + departure.lat + ", lng=" + departure.lng);
        System.out.println("Arrival:   lat=" + arrival.lat + ", lng=" + arrival.lng);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Goog-Api-Key", apiKey);
        headers.set("X-Goog-FieldMask", "routes.distanceMeters,routes.polyline.encodedPolyline");

        Map<String, Object> body = Map.of(
                "origin", Map.of(
                        "location", Map.of(
                                "latLng", Map.of(
                                        "latitude", departure.lat,
                                        "longitude", departure.lng
                                )
                        )
                ),
                "destination", Map.of(
                        "location", Map.of(
                                "latLng", Map.of(
                                        "latitude", arrival.lat,
                                        "longitude", arrival.lng
                                )
                        )
                ),
                "travelMode", "DRIVE",
                "routingPreference", "TRAFFIC_AWARE",
                "computeAlternativeRoutes", false,
                "routeModifiers", Map.of(
                        "avoidTolls", false,
                        "avoidHighways", false,
                        "avoidFerries", false
                ),
                "languageCode", "en-US",
                "units", "METRIC"
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            Map<String, Object> responseBody = response.getBody();

            if (responseBody == null || !responseBody.containsKey("routes")) {
                throw new RuntimeException("Google Routes API error: " + responseBody);
            }

            List<Map<String, Object>> routes = (List<Map<String, Object>>) responseBody.get("routes");
            Map<String, Object> route = routes.get(0);

            double distanceKm = ((Number) route.get("distanceMeters")).doubleValue() / 1000.0;
            String polyline = (String) ((Map<String, Object>) route.get("polyline")).get("encodedPolyline");

            return new RouteInfoDTO(polyline, distanceKm);

        } catch (HttpStatusCodeException ex) {
            String errorbody = ex.getResponseBodyAsString();
            int statusCode = ex.getRawStatusCode();
            throw new RuntimeException("Google Routes API HTTP " + statusCode + ": " + errorbody, ex);
        } catch (RestClientException e) {
            throw new RuntimeException("Route computation failed: " + e.getMessage(), e);
        }
    }



}

