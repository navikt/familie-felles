# sikkerhet

Felles sikkerhetsbibliotek for Familie-applikasjoner. Inneholder `TokenContext`-APIet og hjelpeklasser for å lese JWT-claims, validere tokens og beskytte endepunkter.

## Innhold

| Klasse | Beskrivelse |
|--------|-------------|
| `TokenContextHolder` | Statisk inngangsport for å lese claims og tokens fra innkommende JWT |
| `EksternBrukerUtils` | Hjelpemetoder for eksterne brukere (tokenx / selvbetjening) |
| `OIDCUtil` | Hjelpeklasse for Nav-ansatte (azuread) — henter NAVident, groups, claims o.l. |
| `ClientTokenValidationFilter` | Servlet-filter som validerer client credentials / on-behalf-of |
| `AuthorizationFilter` | Servlet-filter som tillater kun registrerte klient-IDer (`azp`) |

## Kom i gang

Legg til avhengigheten i `pom.xml`:

```xml
<dependency>
    <groupId>no.nav.familie.felles</groupId>
    <artifactId>sikkerhet</artifactId>
    <version>${familie-felles.version}</version>
</dependency>
```

## Krav: TokenContext-implementasjon

Appen må importere nøyaktig én `TokenContext`-implementasjon. Spring feiler ved oppstart med `TokenContextKonfigurasjonException` hvis ingen eller flere importeres.

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

> Importer kun én av disse — Spring feiler ved oppstart hvis begge importeres.

## Bruk

### Lese claims (NAV-ansatte / azuread)

```kotlin
val navIdent = TokenContextHolder.getClaimAsString("NAVident")
val groups   = TokenContextHolder.getClaimAsStringList("groups")
val token    = TokenContextHolder.getBearerToken("azuread")
```

Eller via Spring-beanen `OIDCUtil`:

```kotlin
@Autowired lateinit var oidcUtil: OIDCUtil

val navIdent = oidcUtil.navIdent
val groups   = oidcUtil.groups
```

### Lese FNR (eksterne brukere / tokenx)

```kotlin
val fnr = EksternBrukerUtils.hentFnrFraToken()
val erSammeBruker = EksternBrukerUtils.personIdentErLikInnloggetBruker(fnr)
```

### Beskytte endepunkter med `ClientTokenValidationFilter`

```kotlin
@Bean
fun clientTokenValidationFilter(): FilterRegistrationBean<ClientTokenValidationFilter> =
    FilterRegistrationBean(
        ClientTokenValidationFilter(
            acceptClientCredential = true,
            acceptOnBehalfOf = false,
        )
    ).apply { addUrlPatterns("/api/intern/*") }
```
