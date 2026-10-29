package app.services;

import app.dto.OpenRouteDTO;
import app.dto.ValidatedAddress;
import app.exceptions.ApiException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

public class OpenRouteService
        implements AddressService {

    private static final String URL =
            "https://api.openrouteservice.org/geocode/search";

    private final String apiKey;

    private final HttpClient client =
            HttpClient.newHttpClient();

    private final ObjectMapper mapper =
            new ObjectMapper();

    public OpenRouteService(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public ValidatedAddress validate(String address) {
        if (address == null || address.isBlank()) {
            throw new ApiException(400, "Destination address is required");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new ApiException(500, "OpenRouteService API key is missing");
        }

        String encodedAddress = URLEncoder.encode(address.trim(), StandardCharsets.UTF_8);
        String requestUrl = URL
                + "?text=" + encodedAddress
                + "&boundary.country=DK"
                + "&layers=address"
                + "&size=1";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(requestUrl))
                .timeout(Duration.ofSeconds(10))
                .header("Authorization", apiKey)
                .header("Accept", "application/json")
                .GET()
                .build();

        try {
            HttpResponse<String> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ApiException(502, "Address service returned status " + response.statusCode());
            }

            OpenRouteDTO result = mapper.readValue(response.body(), OpenRouteDTO.class);
            if (result.getFeatures() == null || result.getFeatures().isEmpty()) {
                throw new ApiException(400, "Address could not be found");
            }

            OpenRouteDTO.Feature feature = result.getFeatures().getFirst();
            if (feature.getProperties() == null || feature.getGeometry() == null) {
                throw new ApiException(502, "Address service returned incomplete data");
            }

            List<Double> coordinates = feature.getGeometry().getCoordinates();
            if (coordinates == null || coordinates.size() < 2) {
                throw new ApiException(502, "Address service returned invalid coordinates");
            }

            return new ValidatedAddress(
                    feature.getProperties().getLabel(),
                    coordinates.get(1),
                    coordinates.get(0)
            );
        } catch (ApiException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ApiException(502, "Address validation was interrupted");
        } catch (IOException | RuntimeException exception) {
            throw new ApiException(502, "Could not validate address");
        }
    }
}
