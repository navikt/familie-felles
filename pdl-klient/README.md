# pdl-klient

Klient for oppslag i PDL (Persondataløsningen) med GraphQL. Appen eier spørringene og DTO-ene. Klienten sender
spørringen, kontrollerer svaret og returnerer `data` som ønsket type.

## Dokumentasjon for tjenesten

| Hva               | Lenke                          |
|-------------------|--------------------------------|
| PDL-dokumentasjon | https://pdl-docs.ansatt.nav.no |
| Slack             | #pdl                           |

## Kom i gang

Legg til avhengigheten:

```xml

<dependency>
    <groupId>no.nav.familie.felles</groupId>
    <artifactId>pdl-klient</artifactId>
</dependency>
```

Modulen slås på når avhengigheten legges til. Sett miljø og, hvis appen alltid bruker samme behandlingsnummer,
behandlingsnummer:

```yaml
familie:
  felles:
    klient:
      miljo: preprod # eller prod
      pdl:
        behandlingsnummer: <behandlingsnummer>
```

| Property                                      | Påkrevd                                         | Beskrivelse                                                                                      |
|-----------------------------------------------|-------------------------------------------------|--------------------------------------------------------------------------------------------------|
| `familie.felles.klient.miljo`                 | Ja, med mindre `url` og `scope` er satt         | `preprod` eller `prod`. Velger URL og scope for PDL.                                             |
| `familie.felles.klient.pdl.enabled`           | Nei                                             | Sett til `false` for å slå av modulen. Standard er `true`.                                       |
| `familie.felles.klient.pdl.behandlingsnummer` | Nei, se [Behandlingsnummer](#behandlingsnummer) | Sendes som headeren `Behandlingsnummer`. Behandlingsnummeret må være registrert for appen i PDL. |
| `familie.felles.klient.pdl.url`               | Nei                                             | Overstyrer URL-en fra `miljo`. Må bruke http eller https. Bruk http bare lokalt og i tester.     |
| `familie.felles.klient.pdl.scope`             | Nei                                             | Overstyrer scope fra `miljo`, på formen `api://<cluster>.<namespace>.<app>/.default`.            |
| `familie.felles.klient.pdl.connect-timeout`   | Nei                                             | Standard 2 sekunder. Mellom 1 ms og `Int.MAX_VALUE` ms.                                          |
| `familie.felles.klient.pdl.read-timeout`      | Nei                                             | Standard 10 sekunder. Mellom 1 ms og `Int.MAX_VALUE` ms.                                         |

Klienten henter token gjennom `token-klient`. Appen trenger derfor miljøvariablene `NAIS_TOKEN_ENDPOINT` og
`NAIS_TOKEN_EXCHANGE_ENDPOINT`, som Nais setter når Entra ID er aktivert.

Appen må ha outbound-tilgang til PDL i Nais-manifestet:

```yaml
accessPolicy:
  outbound:
    external:
      - host: pdl-api.dev-fss-pub.nais.io # pdl-api.prod-fss-pub.nais.io i prod
```

## Klientene

Autokonfigurasjonen lager én klasse per tokentype. Appen velger klient ved å injisere klassen, ikke med `@Qualifier`:

| Klasse            | Token                                  | Uten brukertoken                  | Krever                                |
|-------------------|----------------------------------------|-----------------------------------|---------------------------------------|
| `PdlOboKlient`    | OBO                                    | Kallet stopper med `PdlException` | En `KlientTokenProvider`-bean i appen |
| `PdlHybridKlient` | OBO når brukertoken finnes, ellers M2M | Bruker maskintoken                | En `KlientTokenProvider`-bean i appen |
| `PdlMaskinKlient` | M2M                                    | Bruker alltid maskintoken         | Ingenting ekstra                      |

Bruk `PdlOboKlient` der PDL skal kontrollere saksbehandlerens tilgang, `PdlMaskinKlient` i bakgrunnsjobber og
systemkall, og `PdlHybridKlient` der samme kode kjøres i begge situasjonene.

```kotlin
class PersonKlient(
    private val pdlKlient: PdlOboKlient,
)
```

Alle tre arver fra `PdlKlient`, som har `hent`. Injiser ikke `PdlKlient` direkte. Da finner Spring tre kandidater.

`PdlOboKlient` og `PdlHybridKlient` opprettes bare når appen har registrert en `KlientTokenProvider`. Den skal returnere
innlogget brukers token, eller `null` når det ikke finnes en bruker:

```kotlin
import no.nav.familie.felles.klient.KlientTokenProvider

@Bean
fun klientTokenProvider(): KlientTokenProvider = KlientTokenProvider { SikkerhetContext.hentJwt()?.tokenValue }
```

Injiserer appen en av disse to klassene uten en slik bean, feiler oppstarten.

> **Hybridklienten faller tilbake til maskin-til-maskin.** Hvis provideren gir `null`, eller tokenet mangler
> `preferred_username`, bruker klienten appens eget token. Da kontrollerer ikke PDL saksbehandlerens tilgang til
> adressebeskyttede eller skjermede personer. Det skjer for eksempel i asynkrone tråder der sikkerhetskonteksten
> mangler.
> Bruk `PdlOboKlient` når kallet forutsetter en innlogget bruker. Bruker du hybridklienten, må appen selv sjekke
> tilgangen
> til personen, for eksempel med Tilgangsmaskinen.

En klient kan erstattes ved å registrere en egen bean av samme klasse.

## Bruk

Spørringen legges som en fil på classpath:

```graphql
# src/main/resources/pdl/hent-person.graphql
query HentPerson($ident: ID!) {
    person: hentPerson(ident: $ident) {
        navn {
            fornavn
        }
    }
}
```

DTO-ene speiler svaret, og klienten henter dem:

```kotlin
data class PdlHentPersonRespons(
    val person: PdlPerson?,
)

data class PdlPerson(
    val navn: List<PdlNavn>,
)

data class PdlNavn(
    val fornavn: String,
)

class PersonKlient(
    private val pdlKlient: PdlOboKlient,
) {
    fun hentPerson(personident: String): PdlPerson? =
        pdlKlient
            .hent<PdlHentPersonRespons>(
                query = ClassPathResource("pdl/hent-person.graphql").getContentAsString(Charsets.UTF_8),
                variabler = mapOf("ident" to personident),
            ).person
}
```

Feltnavnet i DTO-en må stemme med navnet i svaret. Spørringen bruker derfor aliaset `person: hentPerson(ident: $ident)`.

Send spørringen uendret. Ikke fjern linjeskift. GraphQL ignorerer dem uansett, og uten linjeskift kan ord på hver sin
linje bli slått sammen, eller en `#`-kommentar kommentere ut resten av spørringen.

`graphqlUrl` på klienten gir adressen kallene går til, for eksempel til logging.

`hent` returnerer innholdet i `data` som typen du ber om. Generiske typer som `List<PdlPerson>` og
`Map<String, List<PdlPerson>>` støttes.

**La DTO-ene følge nullbarheten i PDL-skjemaet.** Felt som PDL kan sende som `null`, må være nullable i DTO-en. Ved for
eksempel `not_found` svarer PDL med `{"person": null}` sammen med feilen. Er `person` påkrevd i DTO-en, feiler
lesingen av svaret. Da får du `Svar fra PDL kunne ikke leses: [data.person]` i stedet for feilkoden fra PDL.

Klienten validerer ikke enkeltfelt i `data`. Appen avgjør selv om for eksempel `person` eller navn kan mangle.

### Behandlingsnummer

Hvert kall sender headeren `behandlingsnummer`. Apper med fast behandlingsnummer setter
`familie.felles.klient.pdl.behandlingsnummer`. Apper der behandlingsnummeret varierer, sender det med i kallet:

```kotlin
pdlKlient.hent<PdlHentPersonRespons>(
    query = query,
    variabler = mapOf("ident" to personident),
    behandlingsnummer = "B123",
)
```

Et behandlingsnummer i kallet overstyrer property-en for det ene kallet. Parameteren kan ikke være `null`, så en
verdi som mangler i appen faller ikke stille tilbake til property-en. Kaller appen `hent` uten behandlingsnummer og
property-en ikke er satt, kaster `hent` `IllegalStateException` før kallet sendes. Et blankt behandlingsnummer i kallet
gir `IllegalArgumentException`.
Dette er programmeringsfeil, så de kastes ikke som `PdlException`.

## Feil

Feil fra kallet mot PDL kastes som `PdlException`:

| Situasjon                                           | `message`                                                     | `errors`             | `httpStatus` | `cause`                               |
|-----------------------------------------------------|---------------------------------------------------------------|----------------------|--------------|---------------------------------------|
| Svaret har `errors`, også sammen med delvise data   | `Svar fra PDL inneholder feil`                                | Alle feilene fra PDL | `null`       | `null`                                |
| HTTP 4xx eller 5xx                                  | `Kall mot PDL feilet med HTTP-status <status>`                | Tom                  | Statuskoden  | Springs `RestClientResponseException` |
| Svaret kan ikke leses som typen du ber om           | `Svar fra PDL kunne ikke leses: [<sti>]`                      | Tom                  | `null`       | `null`                                |
| Svaret mangler body eller `data`                    | `Svar fra PDL mangler body` eller `Svar fra PDL mangler data` | Tom                  | `null`       | `null`                                |
| Nettverks-, token- eller manglende brukertoken-feil | `Kall mot PDL feilet`                                         | Tom                  | `null`       | Den opprinnelige feilen               |

Klienten tolker ikke feilkodene. Sjekk `extensions.code` selv, for eksempel:

```kotlin
catch(feil: PdlException) {
    if (feil.errors.any { it.extensions?.code == "not_found" }) {
        // personen finnes ikke
    }
}
```

`PdlFeil` inneholder `message`, `locations`, `path` og `extensions` med `code`, `details` og `classification`. Ukjente
felt ignoreres. `feilmelding` setter sammen `message` og alle meldingene fra PDL.

`PdlRespons` leser også advarsler fra `extensions.warnings` inn i `PdlAdvarsel` med `query`, `id`, `code`, `message`
og `details`. `hent` returnerer bare `data`, så appen får ikke advarslene ennå. PDL bruker advarsler blant annet når
behandlingsnummeret ikke gir tilgang til alle opplysningene i spørringen. Da kan opplysningene mangle i `data` uten at
svaret har `errors`.

`<sti>` er feltstien til verdien som ikke kunne leses, for eksempel `data.hentPerson.foedselsdato.[0].foedselsdato`.
Selve verdien tas ikke med, og den opprinnelige feilen legges ikke ved som `cause`, fordi den kan inneholde verdier fra
svaret.

## Logging og personopplysninger

Klienten logger ikke selv. Appen har ansvaret for hva som logges.

- `PdlFeil.message` inneholder ifølge PDL ikke personopplysninger og kan logges.
- `cause` ved HTTP-feil inneholder svarbodyen fra PDL. Ved nettverks- og tokenfeil inneholder den den opprinnelige
  feilen. Vurder hva som skal i vanlig logg og hva som skal i secureLogs.
- `data` inneholder personopplysninger. `PdlRespons`, `PdlFeil`, `PdlFeilExtensions` og `PdlAdvarsel` er data-klasser
  uten skjult `toString()`, og appens egne DTO-er bør skjule personopplysninger i `toString()`.
- Spørringen og variablene, som ofte inneholder en personident, sendes aldri med i feilene.

## Retry

Klienten prøver på nytt ved midlertidige feil. Retry er på som standard og gjelder alle tre klientvariantene.
Bruk klienten bare til GraphQL-queries som leser data, ikke mutations. Mekanismen er grunnklassen `RetryKlient` fra
`klient-felles`, som `PdlKlient` arver. Den bygger på Spring `RetryTemplate` og `RetryPolicy`, krever ingen
Spring-proxy og virker også når klienten opprettes med en konstruktør.

Følgende properties finnes under `familie.felles.klient.pdl.retry`:

| Property      | Standard | Tillatte verdier                                        |
|---------------|----------|---------------------------------------------------------|
| `enabled`     | `true`   | `true` eller `false`                                    |
| `max-retries` | `2`      | 1–5 nye forsøk etter første kall, altså 2–6 kall totalt |
| `delay`       | `100ms`  | Mellom 1 ms og 1 sekund                                 |

Innstillingene bindes og valideres ved oppstart. Ventetiden mellom forsøkene er konstant `delay`, med jitter på inntil
`delay / 2`. Jitter legges bare til, aldri trukket fra. Med standardoppsettet venter klienten 100–150 ms før hvert nye
forsøk. Bruk `enabled=false` for å slå av retry. Har appen egen retry rundt `hent`, multipliseres antall kall. Svarer
PDL tregt med 502, 503 eller 504, kan samlet ventetid med standardoppsettet bli over 30 sekunder med standard
read-timeout på 10 sekunder.

Klienten gjentar hele kallet ved HTTP 502, 503 og 504, uansett om feilen kommer fra PDL eller fra tokenhentingen.
Statuskodene er ikke konfigurerbare. Klienten vurderer bare feilen på toppnivå. Transportfeil, andre HTTP-statuser,
GraphQL-feil, deserialiseringsfeil og feil under body-lesing etter HTTP 200 gjentas ikke.

Hvert nytt forsøk går gjennom alle interceptorene på `RestClient` på nytt, også tokenhentingen. Interceptorene må
derfor tåle å bli kjørt flere ganger. Tokenklienten beholder sin egen retry med to forsøk ved 5xx og transportfeil.
Ved vedvarende 502, 503 eller 504 fra token-endpointet blir det derfor opptil seks tokenkall med standardoppsettet.
Når feilen kommer fra tokenhentingen, får `PdlException` likevel meldingen «Kall mot PDL feilet med HTTP-status …»
og token-endpointets status i `httpStatus`.

Etter siste forsøk kastes `PdlException` med samme feilkontrakt som ved ett forsøk.
Trådavbrudd (interrupt) under ventetiden stopper nye forsøk. Klienten kaster da siste feil, og interrupt-status
forblir satt. En pågående socket-lesing avbrytes ikke nødvendigvis.
Tidligere forsøk legges ikke til som suppressed exceptions i feilen som returneres til konsumenten.
Spring `RetryTemplate` logger hvert mislykkede forsøk med exception og stacktrace på DEBUG-nivå. For HTTP-feil kan
meldingen inneholde responsbodyen fra PDL eller token-endpointet. Ikke slå på DEBUG for `org.springframework.core.retry`
i produksjon.
