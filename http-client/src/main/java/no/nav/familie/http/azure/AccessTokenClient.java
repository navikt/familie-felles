package no.nav.familie.http.azure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static java.time.LocalDateTime.now;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;

public class AccessTokenClient {

    private static final Logger logger = LoggerFactory.getLogger(AccessTokenClient.class);
    private final URI aadAccessTokenUrl;
    private final String clientId;
    private final String clientSecret;

    private final RestTemplate restTemplate;
    private AccessTokenDto cachedToken;
    private LocalDateTime cachedTokenExpiresOn;

    public AccessTokenClient(URI aadAccessTokenUrl,
                             String clientId,
                             String clientSecret,
                             RestTemplate restTemplate) {
        this.aadAccessTokenUrl = aadAccessTokenUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.restTemplate = restTemplate;
    }

    private boolean isTokenValid() {
        if (cachedToken == null) {
            return false;
        }

        logger.debug("Tokenet løper ut: {}. Tiden nå er: {}", cachedTokenExpiresOn, now(ZoneId.systemDefault()));
        return cachedTokenExpiresOn.minusMinutes(5).isAfter(now(ZoneId.systemDefault()));
    }

    public AccessTokenDto getAccessToken(String scope) {
        if (isTokenValid()) {
            logger.debug("Henter token fra cache");
            return cachedToken;
        }

        logger.debug("Henter token fra azure");

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("client_id", clientId);
            body.add("scope", scope);
            body.add("grant_type", "client_credentials");
            body.add("client_secret", clientSecret);

            HttpEntity<MultiValueMap<String, String>> httpEntity = new HttpEntity<>(body, headers);

            ResponseEntity<AccessTokenDto> accessTokenResponse =
                restTemplate.exchange(aadAccessTokenUrl, HttpMethod.POST, httpEntity, AccessTokenDto.class);

            if (accessTokenResponse.getStatusCode() == HttpStatus.OK) {
                AccessTokenDto accessToken = accessTokenResponse.getBody();

                if (accessToken != null) {
                    this.cachedToken = accessToken;
                    this.cachedTokenExpiresOn =
                        LocalDateTime.now(ZoneId.systemDefault()).plusSeconds(accessToken.getExpires_in());
                    return accessToken;
                }
                logger.warn("Manglende token fra azure ad");
                throw new AzureAccessTokenException("Manglende token fra azure ad");
            }
            logger.warn("Kall mot azure ad for å hente token feilet");
            throw new AzureAccessTokenException("Kall mot azure ad for å hente token feilet");
        } catch (RestClientException e) {
            logger.warn("Kall mot azure ad for å hente token feilet");
            throw new AzureAccessTokenException("Kall mot azure ad for å hente token feilet", e);
        }
    }
}
