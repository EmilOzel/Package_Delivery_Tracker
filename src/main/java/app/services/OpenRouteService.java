package app.services;

import app.dto.OpenRouteDTO;
import app.dto.ValidatedAddress;
import app.exceptions.ApiException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class OpenRouteService implements AddressService {

    private static final String URL = "https://api.openrouteservice.org/geocode/search";

    private final String apiKey;
    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    public OpenRouteService(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public ValidatedAddress validate(String address) {
        if (address == null) {
            throw new ApiException(400, "Destination address is required");
        }

        if (address.isBlank()) {
            throw new ApiException(400, "Destination address is required");
        }

        String encodedAddress = URLEncoder.encode(address.trim(), StandardCharsets.UTF_8);
        String requestUrl = URL
                + "?text=" + encodedAddress
                + "&boundary.country=DK"
                + "&layers=address"
                + "&size=1";

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder();
        requestBuilder.uri(URI.create(requestUrl));
        requestBuilder.header("Authorization", apiKey);
        requestBuilder.GET();
        HttpRequest request = requestBuilder.build();

        try {
            HttpResponse<String> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            int statusCode = response.statusCode();
            if (statusCode < 200) {
                throw new ApiException(502, "Address service returned status " + statusCode);
            }

            if (statusCode >= 300) {
                throw new ApiException(502, "Address service returned status " + statusCode);
            }

            OpenRouteDTO result = mapper.readValue(
                    response.body(),
                    OpenRouteDTO.class
            );

            if (result.getFeatures() == null) {
                throw new ApiException(400, "Address could not be found");
            }

            if (result.getFeatures().isEmpty()) {
                throw new ApiException(400, "Address could not be found");
            }

            OpenRouteDTO.Feature feature = result.getFeatures().getFirst();
            String formattedAddress = feature.getProperties().getLabel();
            double latitude = feature.getGeometry().getCoordinates().get(1);
            double longitude = feature.getGeometry().getCoordinates().get(0);

            return new ValidatedAddress(
                    formattedAddress,
                    latitude,
                    longitude
            );
        } catch (ApiException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ApiException(502, "Address validation was interrupted");
        } catch (Exception exception) {
            throw new ApiException(502, "Could not validate address");
        }
    }
}
