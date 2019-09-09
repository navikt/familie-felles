package no.nav.familie.http.sts;

public class StsAccessTokenFeilException extends RuntimeException {

    public StsAccessTokenFeilException(String message, Throwable cause) {
        super(message, cause);
    }

    public StsAccessTokenFeilException(String message) {
        super(message);
    }
}
