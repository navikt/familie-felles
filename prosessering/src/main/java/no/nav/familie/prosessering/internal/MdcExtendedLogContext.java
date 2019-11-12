package no.nav.familie.prosessering.internal;

import org.slf4j.MDC;

import java.util.*;
import java.util.regex.Pattern;

/**
 * {@link MDC} backet parameter som tillater en semi-colon separert liste av sub-keys.
 * Kan dermed legge til og fjerne ekstra-kontekst data dynamisk.
 */
class MdcExtendedLogContext {

    private static final Pattern ILLEGAL_CHARS = Pattern.compile("[\\[\\];=]");
    private String paramName;
    private String baseFormat;

    private MdcExtendedLogContext(String paramName) {
        Objects.requireNonNull(paramName, "paramName");
        this.paramName = paramName;
        this.baseFormat = paramName + "[]";
    }

    static MdcExtendedLogContext getContext(String kontekstParamNavn) {
        return new MdcExtendedLogContext(kontekstParamNavn);
    }

    void add(String key, Object value) {
        String currentValue = mdcKey();
        String cleanedValue = removeKeyValue(key, currentValue);
        String newValue = insertValue(cleanedValue, key, value);
        MDC.put(paramName, newValue);
    }

    public String getFullText() {
        return MDC.get(paramName);
    }

    private String insertValue(String currentValue, String key, Object keyValue) {
        validateKey(key);
        String val = currentValue.substring(0, currentValue.length() - 1);
        val = val + (val.endsWith("[") ? "" : ";") + key + "=" + keyValue + "]";
        return val;
    }

    private void validateKey(String key) {
        if (key == null || ILLEGAL_CHARS.matcher(key).find()) {
            throw new IllegalArgumentException("Ugyldig key: '" + key + "'");
        }
    }

    public String getValue(String key) {
        validateKey(key);
        String currentValue = mdcKey();
        List<String> parts = splitParts(currentValue);
        String keyPart = key + "=";
        for (String part : parts) {
            if (part.startsWith(keyPart)) {
                return part.substring(keyPart.length(), part.length());
            }
        }

        return null;
    }

    public void remove(String key) {
        validateKey(key);
        String currentValue = mdcKey();
        if (currentValue == null) {
            return;
        }
        String newValue = removeKeyValue(key, currentValue);
        MDC.put(paramName, newValue);
    }

    private String mdcKey() {
        String currentValue = MDC.get(paramName);
        return (currentValue != null) ? currentValue : baseFormat;
    }

    private String removeKeyValue(String key, String orgValue) {
        if (orgValue == null) {
            return null;
        }

        List<String> contentList = splitParts(orgValue);

        int orgSize = contentList.size();
        for (int i = orgSize; --i >= 0; ) {
            if (contentList.get(i).startsWith(key + "=")) {
                contentList.remove(i);
            }
        }
        if (orgSize == contentList.size()) {
            return orgValue;
        } else if (contentList.isEmpty()) {
            return null;
        } else {
            return paramName + "[" + String.join(";", contentList) + "]";
        }

    }

    private List<String> splitParts(String orgValue) {
        if (orgValue == null) {
            return Collections.emptyList();
        }
        String content = orgValue.substring(paramName.length() + 1, orgValue.length() - 1);
        return new ArrayList<>(Arrays.asList(content.split(";")));
    }

    void clear() {
        MDC.remove(paramName);
    }
}
