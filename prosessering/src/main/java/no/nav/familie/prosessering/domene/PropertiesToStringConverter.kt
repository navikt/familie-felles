package no.nav.familie.prosessering.domene

import java.io.IOException
import java.io.StringReader
import java.io.StringWriter
import java.util.*
import javax.persistence.AttributeConverter
import javax.persistence.Converter

/**
 * JPA konverterer for å skrive ned en key=value text til et databasefelt (output tilsvarer java.util.Properties
 * format).
 */
@Converter
class PropertiesToStringConverter : AttributeConverter<Properties, String> {

    override fun convertToDatabaseColumn(props: Properties?): String? {
        if (props == null || props.isEmpty) {
            return null
        }
        val stringWriter = StringWriter(512)
        // custom i stedet for Properties.store slik at vi ikke får med default timestamp
        props.forEach { key, value -> stringWriter.append(key as String).append('=').append(value as String).append('\n') }
        return stringWriter.toString()
    }

    override fun convertToEntityAttribute(dbData: String?): Properties {
        val props = Properties()
        if (dbData != null) {
            try {
                props.load(StringReader(dbData))
            } catch (e: IOException) {
                throw IllegalArgumentException("Kan ikke lese properties til string:$props", e) //$NON-NLS-1$
            }
        }
        return props
    }
}
