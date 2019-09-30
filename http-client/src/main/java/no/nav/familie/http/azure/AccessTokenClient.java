package no.nav.familie.http.azure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    public String getAccessToken(String resource) {
        if (isTokenValid()) {
            logger.debug("Henter token fra cache");
            return cachedToken.getAccess_token();
        }

        logger.debug("Henter token fra azure");

        AccessTokenRequestBody accessTokenRequestBody = new AccessTokenRequestBody(clientId, resource, grantType, clientSecret);
        HttpEntity<AccessTokenRequestBody> httpEntity = new HttpEntity<>(accessTokenRequestBody);
        ResponseEntity<AccessTokenDto> accessTokenResponse = restTemplate.exchange(aadAccessTokenUrl, HttpMethod.POST, httpEntity, AccessTokenDto.class);

        if (accessTokenResponse.getStatusCode() == HttpStatus.OK) {
            AccessTokenDto accessToken = accessTokenResponse.getBody();

            if (accessToken != null) {
                this.cachedToken = accessToken;
                return accessToken.access_token;
            } else {
                throw new AzureAccessTokenException("Manglende token");
            }
        } else {
            throw new AzureAccessTokenException("Kall for å hente azure token feilet");
        }
    }
}
