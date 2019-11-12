package no.nav.familie.prosessering.domene;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Properties;

/**
 * JPA konverterer for å skrive ned en key=value text til et databasefelt (output tilsvarer java.util.Properties
 * format).
 */
@Converter
public class PropertiesToStringConverter implements AttributeConverter<Properties, String> {

    @Override
    public String convertToDatabaseColumn(Properties props) {
        if (props == null || props.isEmpty()) {
            return null;
        }
        StringWriter sw = new StringWriter(512);
        // custom istdf Properties.store slik at vi ikke får med default timestamp
        props.forEach((k, v) -> {
            sw.append((String) k).append('=').append((String) v).append('\n');
        });
        return sw.toString();

    }

    @Override
    public Properties convertToEntityAttribute(String dbData) {
        Properties props = new Properties();
        if (dbData != null) {
            try {
                props.load(new StringReader(dbData));
            } catch (IOException e) {
                throw new IllegalArgumentException("Kan ikke lese properties til string:" + props, e); //$NON-NLS-1$
            }
        }
        return props;
    }
}
