# http-client

HTTP-klientbibliotek basert på Spring `RestTemplate`. Tilbyr abstrakte baseklasser for REST- og SOAP-klienter, samt interceptorer for Bearer-token-autentisering mot interne Nav-tjenester.

## Innhold

| Klasse | Beskrivelse |
|--------|-------------|
| `AbstractRestClient` | Baseklasse for typede REST-klienter med feilhåndtering og logging |
| `AbstractPingableRestClient` | Utvider `AbstractRestClient` med helsesjekk (`/ping`) |
| `BearerTokenClientInterceptor` | Henter access token automatisk og setter `Authorization: Bearer`-header |
| `BearerTokenClientCredentialsClientInterceptor` | Som over, men tvinger client credentials grant |
| `BearerTokenOnBehalfOfClientInterceptor` | Som over, men tvinger on-behalf-of (JWT bearer) grant |
| `BearerTokenExchangeClientInterceptor` | Som over, men tvinger token exchange grant |
| `ConsumerIdClientInterceptor` | Setter `Nav-Consumer-Id`-header |
| `MdcValuesPropagatingClientInterceptor` | Propagerer MDC-verdier (correlation ID o.l.) til utgående kall |

## Kom i gang

Legg til avhengigheten i `pom.xml`:

```xml
<dependency>
    <groupId>no.nav.familie.felles</groupId>
    <artifactId>http-client</artifactId>
    <version>${familie-felles.version}</version>
</dependency>
```

## Krav: TokenContext-implementasjon

`BearerTokenClientInterceptor` bruker `TokenContextHolder` for å avgjøre om en forespørsel kommer fra et system (ingen innlogget bruker) eller en bruker (on-behalf-of). Uten en konfigurert `TokenContext`-implementasjon kaster interceptoren en `TokenContextKonfigurasjonException` ved oppstart.

Legg til én av følgende avhengigheter og importer tilhørende konfigurasjonsklasse:

**Spring Security OAuth2 Resource Server**:

```xml
<dependency>
    <groupId>no.nav.familie.felles</groupId>
    <artifactId>sikkerhet-spring-security</artifactId>
    <version>${familie-felles.version}</version>
</dependency>
```

```kotlin
@Import(FamilieFellesSpringSecurityKonfigurasjon::class)
@SpringBootApplication
class MinApp
```

**Nav token-support**:

```xml
<dependency>
    <groupId>no.nav.familie.felles</groupId>
    <artifactId>sikkerhet-token-support</artifactId>
    <version>${familie-felles.version}</version>
</dependency>
```

```kotlin
@Import(FamilieFellesNavTokenSupportKonfigurasjon::class)
@SpringBootApplication
class MinApp
```

> Importer kun én av disse. Spring kaster feil ved oppstart dersom begge importeres samtidig.

## Bruk

### Implementere en REST-klient

```kotlin
@Service
class MinTjenesteKlient(
    restTemplate: RestTemplate,
    @Value("\${min-tjeneste.url}") baseUrl: String,
) : AbstractPingableRestClient(restTemplate, "min-tjeneste") {

    private val uri = URI.create(baseUrl)

    fun hentData(id: String): MinData =
        getForEntity(uri.resolve("/api/data/$id"), MinData::class.java)
}
```

### Sette opp RestTemplate med interceptorer

```kotlin
@Import(
    BearerTokenClientInterceptor::class,
    ConsumerIdClientInterceptor::class
)
class RestTemplateConfig {
    @Bean
    fun restTemplate(
        bearerTokenClientInterceptor: BearerTokenClientInterceptor,
        consumerIdClientInterceptor: ConsumerIdClientInterceptor,
    ): RestTemplate =
        RestTemplateBuilder()
            .interceptors(
                bearerTokenClientInterceptor,
                consumerIdClientInterceptor,
            ).build()
}
```
