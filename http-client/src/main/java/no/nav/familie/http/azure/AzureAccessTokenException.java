package no.nav.familie.http.azure;

public class AzureAccessTokenException extends RuntimeException {

    public AzureAccessTokenException(String message, Throwable cause) {
        super(message, cause);
    }

    public AzureAccessTokenException(String message) {
        super(message);
    }
}
