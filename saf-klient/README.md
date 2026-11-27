# saf-klient

Klient for lesekall mot Saf (Sak- og arkivfasade) med saksbehandlers OBO-token. Appen eier GraphQL-spørringene,
DTO-ene og pagineringen. Klienten sender spørringen, kontrollerer svaret og returnerer `data` som ønsket type.
Dokumenter hentes som `ByteArray`.

## Dokumentasjon for tjenesten

Saf eies av Team Dokumentløsninger.

| Hva               | Lenke                                                                   |
|-------------------|-------------------------------------------------------------------------|
| Saf-dokumentasjon | [Confluence](https://confluence.adeo.no/spaces/BOA/pages/309563005/saf) |
| Slack             | #team_dokumentløsninger                                                 |

## Kom i gang

Legg til avhengigheten under familie-felles sin versjonsstyring:

```xml

<dependency>
    <groupId>no.nav.familie.felles</groupId>
    <artifactId>saf-klient</artifactId>
</dependency>
```

Modulen slås på når avhengigheten legges til. Sett miljø:

```yaml
familie:
  felles:
    klient:
      miljo: preprod # eller prod
```

| Miljø     | URL                                  | Scope                                                |
|-----------|--------------------------------------|------------------------------------------------------|
| `preprod` | `https://saf-q2.dev-fss-pub.nais.io` | `api://dev-fss.teamdokumenthandtering.saf/.default`  |
| `prod`    | `https://saf.prod-fss-pub.nais.io`   | `api://prod-fss.teamdokumenthandtering.saf/.default` |

| Property                                    | Påkrevd                                 | Beskrivelse                                                                           |
|---------------------------------------------|-----------------------------------------|---------------------------------------------------------------------------------------|
| `familie.felles.klient.miljo`               | Ja, med mindre `url` og `scope` er satt | `preprod` eller `prod`. Velger URL og scope for Saf.                                  |
| `familie.felles.klient.saf.enabled`         | Nei                                     | Sett til `false` for å slå av modulen. Standard er `true`.                            |
| `familie.felles.klient.saf.url`             | Nei                                     | Overstyrer URL-en fra `miljo`, uten `/graphql`. Bruk http bare lokalt og i tester.    |
| `familie.felles.klient.saf.scope`           | Nei                                     | Overstyrer scope fra `miljo`, på formen `api://<cluster>.<namespace>.<app>/.default`. |
| `familie.felles.klient.saf.connect-timeout` | Nei                                     | Standard 2 sekunder. Mellom 1 ms og `Int.MAX_VALUE` ms.                               |
| `familie.felles.klient.saf.read-timeout`    | Nei                                     | Standard 10 sekunder. Mellom 1 ms og `Int.MAX_VALUE` ms.                              |

Konfigurasjonen valideres ved oppstart. Mangler miljø eller nødvendige overstyringer, feiler oppstarten med navnet på
property-en som mangler.

Klienten henter token gjennom `token-klient`. Appen trenger derfor miljøvariablene `NAIS_TOKEN_ENDPOINT` og
`NAIS_TOKEN_EXCHANGE_ENDPOINT`, som Nais setter når Entra ID er aktivert. Fabrikken bruker også `application.name`
for konsumentheaderen.

Appen må være preautorisert hos Saf. Følg Saf-veiledningen og avklar tilgangen med tjenesteeier. Aktiver Entra ID i
Nais-manifestet og legg til outbound-tilgang til Saf:

```yaml
accessPolicy:
  outbound:
    external:
      - host: saf-q2.dev-fss-pub.nais.io # saf.prod-fss-pub.nais.io i prod
```

Biblioteket oppretter ingen innkommende endepunkter eller nettverkstilganger. Behold appens egen
`accessPolicy.inbound`.

## Klientene

Autokonfigurasjonen lager én klasse, `SafOboKlient`. Appen velger klienten ved å injisere klassen:

| Klasse         | Token | Uten brukertoken                             | Krever                                |
|----------------|-------|----------------------------------------------|---------------------------------------|
| `SafOboKlient` | OBO   | Kallet stopper før tokenveksling og Saf-kall | En `KlientTokenProvider`-bean i appen |

```kotlin
class DokumentKlient(
    private val safKlient: SafOboKlient,
)
```

`SafOboKlient` arver fra den sealed klassen `SafKlient`, som har `hent` og `hentDokument`. Injiser ikke `SafKlient`
direkte.

Klienten opprettes bare når appen har registrert en `KlientTokenProvider`. Den skal returnere innlogget saksbehandlers
token, eller `null` når det ikke finnes en bruker. Den må aldri returnere et maskintoken:

```kotlin
import no.nav.familie.felles.klient.KlientTokenProvider

@Bean
fun klientTokenProvider(): KlientTokenProvider = KlientTokenProvider { SikkerhetContext.hentJwt()?.tokenValue }
```

Gir provideren `null`, stopper kallet før tokenveksling og Saf-kall. Et tomt eller blankt token sendes videre og
stoppes av Saf. Det finnes ingen fallback til maskintoken.

En klient kan erstattes ved å registrere en egen `SafOboKlient`-bean. Ved manuell konstruksjon har appen ansvar for at
den injiserte `RestClient` bruker OBO og har timeouter.

## Bruk

Bruk faste spørringer fra classpath og send brukerinput som GraphQL-variabler. Ikke bygg spørringstekst fra input.
Klienten validerer ikke appens forretningsregler.

Spørringen legges som en fil på classpath:

```graphql
# src/main/resources/saf/hent-journalpost.graphql
query HentJournalpost($journalpostId: String!) {
    journalpost(journalpostId: $journalpostId) {
        journalpostId
        tema
        bruker {
            id
            type
        }
    }
}
```

DTO-ene speiler svaret, og klienten henter dem:

```kotlin
data class SafHentJournalpostRespons(
    val journalpost: SafJournalpost?,
)

data class SafJournalpost(
    val journalpostId: String,
    val tema: String?,
    val bruker: SafBruker?,
)

data class SafBruker(
    val id: String?,
    val type: String?,
)

class JournalpostKlient(
    private val safKlient: SafOboKlient,
) {
    fun hentJournalpost(journalpostId: String): SafJournalpost? =
        safKlient
            .hent<SafHentJournalpostRespons>(
                query = ClassPathResource("saf/hent-journalpost.graphql").getContentAsString(Charsets.UTF_8),
                variabler = mapOf("journalpostId" to journalpostId),
            ).journalpost
}
```

`hent` returnerer innholdet i `data` som typen du ber om. Generiske typer som `Map<String, List<SafJournalpost>>`
støttes.

**La DTO-ene følge nullbarheten i Saf-skjemaet.** `journalpost` må være nullable i respons-DTO-en. Ellers kan lesingen
av svaret feile før du får lest en GraphQL-feilkode.

### Dokument

Dokumentet kan hentes med:

```kotlin
val dokument: ByteArray = safKlient.hentDokument(
    journalpostId = journalpostId,
    dokumentInfoId = dokumentInfoId,
)
```

`variantFormat` er `SafVariantFormat.ARKIV` som standard. Send en annen verdi fra `SafVariantFormat` for andre
varianter.

Klienten validerer ikke ID-ene. Journalpost-ID, dokumentinfo-ID og variantformat settes inn som URI-enkodede
path-segmenter. `SafVariantFormat` begrenser variantformatene, og Saf avgjør hvilke varianter saksbehandleren har
tilgang til. Bytes er normalt PDF, men klienten krever ikke PDF-format. Hele dokumentet leses inn i minnet, så appen
må vurdere minnebruk ved store dokumenter og mange samtidige kall. Klienten har ingen grense for dokumentstørrelse.

## Feil

Feil fra kallet mot Saf kastes som `SafException`:

| Situasjon                                         | `message`                                                                      | `errors`             | `httpStatus` | `cause`                               |
|---------------------------------------------------|--------------------------------------------------------------------------------|----------------------|--------------|---------------------------------------|
| Svaret har `errors`, også sammen med delvise data | `Svar fra Saf inneholder feil`                                                 | Alle feilene fra Saf | Utledet      | `null`                                |
| HTTP 4xx eller 5xx                                | `Kall mot Saf feilet med HTTP-status <status>`                                 | Tom                  | Statuskoden  | Springs `RestClientResponseException` |
| Svaret kan ikke leses som typen du ber om         | `Svar fra Saf kunne ikke leses` eller `Svar fra Saf kunne ikke leses: [<sti>]` | Tom                  | `null`       | `null`                                |
| Svaret mangler body eller `data`                  | `Svar fra Saf mangler body` eller `Svar fra Saf mangler data`                  | Tom                  | `null`       | `null`                                |
| Nettverks- eller tokenfeil uten HTTP-status       | `Kall mot Saf feilet`                                                          | Tom                  | `null`       | Den opprinnelige feilen               |

Ved GraphQL-feil svarer Saf med HTTP 200. Klienten utleder da `httpStatus` fra `extensions.code`:

| `extensions.code`    | `httpStatus` |
|----------------------|--------------|
| `forbidden`          | 403          |
| `not_found`          | 404          |
| `bad_request`        | 400          |
| `server_error`       | 500          |
| Ukjent eller mangler | `null`       |

Har svaret flere feil, gjelder rekkefølgen i tabellen, slik at `forbidden` alltid vinner. Ved HTTP-feil er
`httpStatus` den faktiske statusen, som kan komme fra Saf eller tokenendepunktet. Den utledede statusen gir ikke nye
forsøk. Vil du se de faktiske kodene, sjekk `extensions.code`:

```kotlin
catch(feil: SafException) {
    if (feil.errors.any { it.extensions?.code == "not_found" }) {
        // dataene finnes ikke
    }
}
```

`SafFeil` inneholder `message`, `locations`, `path` og `extensions` med `code`, `details` og `classification`. Ukjente
felt ignoreres. `extensions` og `code` kan mangle. Klienten beholder koden som tekst, også ved nye, ukjente koder. Ved
`errors` returnerer klienten aldri delvise data. Et nullable felt i `data`, for eksempel `journalpost: null`, tolkes
ikke alene som `not_found`.

`<sti>` er feltstien til verdien som ikke kunne leses, for eksempel `[data.journalpost.bruker]`. Selve verdien tas ikke
med, og den opprinnelige feilen legges ikke ved som `cause`, fordi den kan inneholde verdier fra svaret. Klienten
undersøker hele årsakskjeden, også når lesefeilen er pakket inn i andre exceptions.

## Logging og personopplysninger

Klienten logger ikke selv. Appen har ansvaret for hva som logges.

- `SafFeil.message` kan inneholde verdier fra spørringen, for eksempel `journalpostId=999999999`.
- `cause` ved HTTP-feil inneholder svarbodyen fra Saf. Ved nettverks- og tokenfeil inneholder den den opprinnelige
  feilen. Vurder hva som skal i vanlig logg og hva som skal i secureLogs.
- `data` og dokumentene inneholder personopplysninger. `SafRequest` og `SafRespons` viser bare klassenavnet i
  `toString()`. `SafFeil` og `SafFeilExtensions` er data-klasser uten skjult `toString()`, og appens egne DTO-er bør
  skjule personopplysninger i `toString()`.
- Spørringen og variablene sendes aldri med i feilene.

## Retry

Klienten prøver på nytt ved midlertidige feil. Retry er på som standard. Alle kallene er lesekall uten bivirkninger.
Mekanismen er grunnklassen `RetryKlient` fra `klient-felles`, som `SafKlient` arver.

Følgende properties finnes under `familie.felles.klient.saf.retry`:

| Property      | Standard | Tillatte verdier                                        |
|---------------|----------|---------------------------------------------------------|
| `enabled`     | `true`   | `true` eller `false`                                    |
| `max-retries` | `2`      | 1–5 nye forsøk etter første kall, altså 2–6 kall totalt |
| `delay`       | `100ms`  | Mellom 1 ms og 1 sekund, med jitter                     |

Klienten gjentar hele HTTP-kallet ved 502, 503 og 504, uansett om feilen kommer fra Saf eller tokenhentingen, og
kjører interceptorene fra `EntraIDRestClientFactory` på nytt. Andre HTTP-statuser, transportfeil, lesefeil og
GraphQL-feil gjentas ikke. Dette gjelder også GraphQL `server_error`, selv om Saf-veiledningen sier at et nytt forsøk
senere kan lykkes. Bruk `enabled=false` for å slå av retry.

Tokenhenteren har egne forsøk ved transportfeil og HTTP 5xx. Ved vedvarende 502, 503 eller 504 fra tokenendepunktet
blir det derfor opptil seks tokenkall med standardoppsettet. Egen retry i appen multipliserer antallet kall.

Spring `RetryTemplate` logger hvert mislykkede forsøk med exception og stacktrace på DEBUG-nivå. For HTTP-feil kan
meldingen inneholde responsbodyen fra Saf eller token-endpointet. Ikke slå på DEBUG for `org.springframework.core.retry`
i produksjon.
