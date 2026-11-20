package no.nav.familie.felles.klient

import org.springframework.boot.context.properties.ConfigurationProperties

enum class KlientMiljo {
    PREPROD,
    PROD,
}

@ConfigurationProperties("familie.felles.klient")
class KlientProperties(
    val miljo: KlientMiljo? = null,
)
