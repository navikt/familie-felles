package no.nav.familie.http.azure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.ZoneId;

import static java.time.LocalTime.now;

public class AccessTokenClient {
    private static final Logger logger = LoggerFactory.getLogger(AccessTokenClient.class);
    private final String grantType = "client_credentials";
    private String aadAccessTokenUrl;
    private String clientId;
    private String clientSecret;

    private RestTemplate restTemplate;
    private AccessTokenDto cachedToken;

    public AccessTokenClient(String aadAccessTokenUrl,
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

        logger.debug("Tokenet løper ut: {}. Tiden nå er: {}", cachedToken.getExpires_on().atZone(ZoneId.systemDefault()).toLocalTime(), now(ZoneId.systemDefault()));
        return cachedToken.getExpires_on()
            .atZone(ZoneId.systemDefault())
            .toLocalTime()
            .minusMinutes(15)
            .isAfter(now(ZoneId.systemDefault()));
    }

    public AccessTokenDto getAccessToken(String resource) {
        if (isTokenValid()) {
            logger.debug("Henter token fra cache");
            return cachedToken;
        }

        logger.debug("Henter token fra azure");

        AccessTokenRequestBody accessTokenRequestBody = new AccessTokenRequestBody(clientId, resource, grantType, clientSecret);
        HttpEntity<AccessTokenRequestBody> httpEntity = new HttpEntity<>(accessTokenRequestBody);

        try {
            ResponseEntity<AccessTokenDto> accessTokenResponse = restTemplate.exchange(aadAccessTokenUrl, HttpMethod.POST, httpEntity, AccessTokenDto.class);

            if (accessTokenResponse.getStatusCode() == HttpStatus.OK) {
                AccessTokenDto accessToken = accessTokenResponse.getBody();

                if (accessToken != null) {
                    this.cachedToken = accessToken;
                    return accessToken;
                } else {
                    logger.warn("Manglende token fra azure ad");
                    throw new AzureAccessTokenException("Manglende token fra azure ad");
                }
            } else {
                logger.warn("Kall mot azure ad for å hente token feilet");
                throw new AzureAccessTokenException("Kall mot azure ad for å hente token feilet" );
            }
        } catch (RestClientException e) {
            logger.warn("Kall mot azure ad for å hente token feilet");
            throw new AzureAccessTokenException("Kall mot azure ad for å hente token feilet", e);
        }
    }
}
