package no.nav.familie.felles.tokenklient.entraid

import no.nav.familie.log.interceptor.ConsumerIdClientInterceptor
import no.nav.familie.log.interceptor.MdcValuesPropagatingClientInterceptor
import org.springframework.context.annotation.Import
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
@Import(
    ConsumerIdClientInterceptor::class,
    MdcValuesPropagatingClientInterceptor::class,
)
class EntraIDRestClientFactory(
    private val entraIDClient: EntraIDClient,
    private val consumerIdClientInterceptor: ConsumerIdClientInterceptor,
    private val mdcValuesPropagatingClientInterceptor: MdcValuesPropagatingClientInterceptor,
) {
    fun lagMaskinTilMaskinRestKlient(target: String): RestClient =
        RestClient
            .builder()
            .requestInterceptor(consumerIdClientInterceptor)
            .requestInterceptor(mdcValuesPropagatingClientInterceptor)
            .requestInterceptor(MaskinTilMaskinTokenInterceptor(entraIDClient, target))
            .build()

    fun lagOboRestKlient(
        target: String,
        tokenSupplier: () -> String,
    ): RestClient =
        RestClient
            .builder()
            .requestInterceptor(consumerIdClientInterceptor)
            .requestInterceptor(mdcValuesPropagatingClientInterceptor)
            .requestInterceptor(OboInterceptor(entraIDClient, target, tokenSupplier))
            .build()

    fun lagHybridRestKlient(
        target: String,
        tokenSupplier: () -> String?,
    ): RestClient =
        RestClient
            .builder()
            .requestInterceptor(consumerIdClientInterceptor)
            .requestInterceptor(mdcValuesPropagatingClientInterceptor)
            .requestInterceptor(MaskinTilMaskinEllerOboInterceptor(entraIDClient, target, tokenSupplier))
            .build()
}
