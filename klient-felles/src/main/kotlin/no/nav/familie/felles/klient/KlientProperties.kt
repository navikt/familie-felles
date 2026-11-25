package no.nav.familie.felles.klient

import org.springframework.boot.context.properties.ConfigurationProperties

enum class KlientMiljø {
    PREPROD,
    PROD,
}

@ConfigurationProperties("familie.felles.klient")
class KlientProperties(
    val miljo: KlientMiljø? = null,
)
