package no.nav.familie.http.client.azure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import no.nav.familie.http.azure.AccessTokenClient;
import no.nav.familie.http.azure.AccessTokenDto;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockserver.junit.MockServerRule;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

public class AzureAccessTokenClientTest {
    private static final int MOCK_SERVER_PORT = 18321;

    @Rule
    public MockServerRule mockServerRule = new MockServerRule(this, MOCK_SERVER_PORT);

    private RestTemplate restTemplate = new RestTemplate();
    private ObjectMapper mapper;

    private AccessTokenClient accessTokenClient = new AccessTokenClient(String.format("http://localhost:%s/token", MOCK_SERVER_PORT), "", "", restTemplate);

    @Before
    public void setUp() {
        mapper = new ObjectMapper();
        JavaTimeModule module = new JavaTimeModule();
        mapper.registerModule(module);
    }

    @Test
    public void skal_validere_token_ok() {
        mockServerRule.getClient()
            .when(
                HttpRequest
                    .request()
                    .withMethod("POST")
                    .withPath("/token")
            )
            .respond(
                HttpResponse.response().withBody(gyldigToken( 60 * 60)).withHeader("Content-Type", "application/json")
            );

        AccessTokenDto accessToken = accessTokenClient.getAccessToken("test");

        assertThat(accessToken.access_token).isNotEmpty();
    }

    @Test
    public void skal_returnere_cachet_token() {
        mockServerRule.getClient()
            .when(
                HttpRequest
                    .request()
                    .withMethod("POST")
                    .withPath("/token")
            )
            .respond(
                HttpResponse.response().withBody(gyldigToken( 60 * 60)).withHeader("Content-Type", "application/json")
            );

        AccessTokenDto accessToken = accessTokenClient.getAccessToken("test");
        AccessTokenDto cachetAccessToken = accessTokenClient.getAccessToken("test");

        assertThat(accessToken.access_token).isNotEmpty();
        assertThat(cachetAccessToken.access_token).isNotEmpty();
        assertThat(accessToken.expires_on).isEqualTo(cachetAccessToken.expires_on);
    }

    private String gyldigToken(int plusSeconds) {
        String jwt = "jwt";
        Instant expiresOn = Instant.now().plusSeconds( plusSeconds );

        try {
            return mapper.writeValueAsString(new AccessTokenDto(jwt, expiresOn));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return "";
        }
    }
}
