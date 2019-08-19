package no.nav.familie.http.client;

import java.util.UUID;

final class IdUtils {
    private IdUtils() {
    }

    static String generateId() {
        UUID uuid = UUID.randomUUID();
        return Long.toHexString(uuid.getMostSignificantBits()) + Long.toHexString(uuid.getLeastSignificantBits());
    }
}

