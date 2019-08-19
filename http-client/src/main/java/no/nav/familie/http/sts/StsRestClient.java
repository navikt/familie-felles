package no.nav.familie.http.sts;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.familie.http.client.HttpClientUtil;
import no.nav.familie.http.client.HttpRequestUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Base64;
import java.util.concurrent.ExecutionException;

import static java.time.LocalTime.now;

public class StsRestClient {

    private static final Logger log = LoggerFactory.getLogger(StsRestClient.class);
    private final HttpClient client;
    private final URI stsUrl;
    private final String stsUsername;
    private final String stsPassword;
    private ObjectMapper mapper;
    private AccessTokenResponse cachedToken;

    public StsRestClient(ObjectMapper mapper, URI stsUrl, String stsUsername, String stsPassword) {
        this.mapper = mapper;
        this.client = HttpClientUtil.create();
        this.stsUrl = stsUrl;
        this.stsUsername = stsUsername;
        this.stsPassword = stsPassword;
    }

    private static String basicAuth(String username, String password) {
        return "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
    }

    private boolean isTokenValid() {
        if (cachedToken == null) {
            return false;
        }

        log.debug("Tokenet løper ut: {}. Tiden nå er: {}", Instant.ofEpochMilli(cachedToken.getExpires_in()).atZone(ZoneId.systemDefault()).toLocalTime(), now(ZoneId.systemDefault()));
        return Instant.ofEpochMilli(cachedToken.getExpires_in())
                .atZone(ZoneId.systemDefault())
                .toLocalTime()
                .minusMinutes(15)
                .isAfter(now(ZoneId.systemDefault()));
    }

    public String getSystemOIDCToken() {
        if (isTokenValid()) {
            log.info("Henter token fra cache");
            return cachedToken.getAccess_token();
        }

        log.info("Henter token fra STS");
        HttpRequest request = HttpRequestUtil.createRequest(basicAuth(stsUsername, stsPassword))
                .uri(stsUrl)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(30))
                .build();

        AccessTokenResponse accessTokenResponse;
        try {
            accessTokenResponse = client
                    .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(HttpResponse::body)
                    .thenApply(it -> {
                        try {
                            return mapper.readValue(it, AccessTokenResponse.class);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        return null;
                    }).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return "";
        }

        if (accessTokenResponse != null) {
            this.cachedToken = accessTokenResponse;
            return accessTokenResponse.getAccess_token();
        } else {
            return "";
        }
    }
}
