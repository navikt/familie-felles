# Oppgave-klient

Typet REST-klient for Oppgaves v1-API, med egne DTO-er og miljøbasert URL og scope.

## Operasjoner

| Klientmetode | HTTP-kall | Returtype |
|---|---|---|
| `finnOppgaver` | `GET /api/v1/oppgaver` | `SokOppgaverResponse` |
| `opprettOppgave` | `POST /api/v1/oppgaver` | `Oppgave` |
| `hentOppgave` | `GET /api/v1/oppgaver/{id}` | `Oppgave` |
| `patchOppgave` | `PATCH /api/v1/oppgaver/{id}` | `Oppgave` |

Et søk henter én side. Konsumenten styrer `limit` og `offset`. Utelatte verdier overlates til Oppgave. Klienten gjentar ikke Oppgave-kall automatisk, heller ikke ved timeout eller versjonskonflikt.

### Oppretting og duplikater

`OpprettOppgaveRequest.uuid` er obligatorisk og har ingen standardverdi. Kode som tidligere utelot UUID-en, må oppdateres til å sende den eksplisitt. Konsumenten må lage én UUID per logisk oppretting og gjenbruke den ved retry. Lagre UUID-en før første POST, slik at retry og omstart bruker samme UUID. Bruk en ny UUID for en annen oppgave, og ikke utled den fra personopplysninger eller korrelasjons-ID.

Ved POST 409 med en gyldig, positiv `oppgaveId` i feilresponsen henter klienten den eksisterende oppgaven med `GET /api/v1/oppgaver/{id}`. `opprettOppgave` returnerer da GET-resultatet som en vanlig `Oppgave`, med samme klientvariant og korrelasjons-ID på begge kall. Den returnerer ikke en ny HTTP 201. Ett opprettingsforsøk kan dermed utføre ett POST og ett GET.

Serveren kontrollerer UUID-en, ikke om requestinnholdet er likt. Gjenbruk av UUID for et annet formål kan derfor returnere feil oppgave. Den gjenfunne oppgaven returneres i sin nåværende tilstand, som kan være endret siden første oppretting. Klienten garanterer ikke «exactly once» eller beskyttelse mot samtidige opprettinger.

Andre 409-responser eller en uleselig duplikatrespons gir original `OppgaveException` uten GET. Hvis GET feiler, kaster klienten GET-kallets exception uendret. PATCH-konflikter gjenfinnes ikke.

Ved 5xx eller timeout må konsumenten selv avgjøre om POST skal prøves igjen, med samme UUID. Klienten utfører ikke automatisk POST-retry eller lager en ny opprettings-UUID.

Tokeninnhentingen kan gjentas én gang ved transportfeil eller HTTP 5xx. Det skjer før Oppgave-kallet og gjentar ikke et kall som allerede er sendt til Oppgave.

Fordeling, ferdigstilling og andre endringer uttrykkes gjennom PATCH. Mapper, kodeverk, hendelser, kommentaroppslag og v2 er utenfor omfanget. Strukturert kommentar i v1-PATCH inngår.

## Klientvarianter

`OppgaveKlient` er et interface med de fire operasjonene. `OppgaveRestKlient` inneholder den felles HTTP-implementasjonen. Alle tre variantene implementerer interfacet gjennom denne klassen. Ingen variant er `@Primary`.

Konsumenten kan avhenge av `OppgaveKlient` og bruke en egen fake i tester, uten HTTP- eller tokenoppsett. Når flere klientbeans finnes i Spring, må auth-variant fortsatt velges eksplisitt, enten med konkret type eller `@Qualifier`:

```kotlin
class Konsument(
    @Qualifier("oppgaveOboKlient") private val oppgaveKlient: OppgaveKlient,
)
```

Når du registrerer en fake som Spring-bean i tester, kan du slå av autokonfigurasjonen med `familie.felles.klient.oppgave.enabled=false`. En fake som bare implementerer interfacet, erstatter ikke automatisk de konkrete klientbeansene.

| Type | Tokenflyt |
|---|---|
| `OppgaveOboKlient` | Krever brukertoken og bruker OBO. |
| `OppgaveHybridKlient` | OBO når tokenet har `preferred_username`, ellers M2M. |
| `OppgaveMaskinKlient` | Bruker alltid M2M. |

`OppgaveOboKlient` og `OppgaveHybridKlient` krever en `KlientTokenProvider`-bean. Den skal levere token fra en allerede validert Entra ID-kontekst. `preferred_username` avgjør tokenvalg, men validerer ikke tokenet.

**Hybrid gir systemtilgang når token mangler, ikke kan parses som JWT eller mangler `preferred_username`.** Dette er tokenvalg, ikke tokenvalidering. Konsumenten må autorisere disse kallene. Velg `OppgaveOboKlient` når handlinger alltid skal utføres på vegne av en medarbeider. Feil ved OBO-utveksling eller avslag fra Oppgave fører ikke til et nytt forsøk med M2M.

Konsumenten har ansvar for behandlingsgrunnlag, tilgang til aktuelle temaer og korrekt bruk av systemtilgang. Tilgang til Oppgave må avtales med Team Oppgavehåndtering.

## Konfigurasjon

Miljø angis med `familie.felles.klient.miljo` (`PREPROD` eller `PROD`).

| Miljø | URL | Scope |
|---|---|---|
| `PREPROD` | `https://oppgave.dev-fss-pub.nais.io` | `api://dev-fss.oppgavehandtering.oppgave/.default` |
| `PROD` | `https://oppgave.prod-fss-pub.nais.io` | `api://prod-fss.oppgavehandtering.oppgave/.default` |

Properties under `familie.felles.klient.oppgave`:

| Property | Standard | Beskrivelse |
|---|---|---|
| `enabled` | `true` | Slår autokonfigurasjonen av/på. |
| `url` | Fra miljø | Valgfri overstyring. HTTP og HTTPS støttes. |
| `scope` | Fra miljø | Valgfri overstyring på formatet `api://cluster.namespace.app/.default`. |
| `connect-timeout` | `2s` | Tilkoblingstimeout. |
| `read-timeout` | `10s` | Lesetimeout. |

Timeouts må være mellom 1 og 2 147 483 647 millisekunder. Uten miljø må både URL og scope settes. Egne beans kan erstatte autokonfigurasjonen per klienttype.

HTTP støttes for lokale miljøer og tester. Bruk HTTPS mot eksterne tjenester og i produksjon. Miljøstandardene bruker HTTPS. Klienten har ingen ekstra protokollsjekk ved manuell oppretting.

Oppsettet bruker `JdkClientHttpRequestFactory`, som støtter PATCH, og følger ikke redirects.

## DTO-er og PATCH

Pakken inneholder egne DTO-er. Responsmodellene ignorerer ukjente felter. Kodeverdier som status, prioritet og tema er strenger slik at nye verdier kan leses uten bibliotekoppgradering. Konsumenten må håndtere ukjente verdier uten å tolke dem som en kjent verdi.

`OpprettOppgaveRequest` og `PatchOppgaveRequest` har en maskert `toString()` som skriver ut typenavnet og ingenting annet. Den beskytter tekstrepresentasjonen som `DefaultRestClient` bruker ved DEBUG-logging av request-body. Faktisk JSON-innhold og serialisering er uendret.

Responsmodellene, `FinnOppgaverRequest` og `PatchFelt` bruker standard tekstrepresentasjon med feltinnhold. Disse representasjonene er ikke maskert og må ikke brukes som logginnhold uten at opplysningene er vurdert og filtrert.

Datoer bruker `LocalDate`, responstidspunkter bruker `OffsetDateTime`, og tidsgrensene i søk bruker `LocalDateTime` på formatet `yyyy-MM-dd'T'HH:mm:ss`.

Jackson 3s standardmapper normaliserer offset ved lesing til UTC. Tidspunktet beholdes, men opprinnelig offset er ikke garantert.

`PatchOppgaveRequest` krever `versjon`. Øvrige felt bruker `PatchFelt<T>`:

| Verdi i Kotlin | Avtalt JSON | Betydning |
|---|---|---|
| `PatchFelt.Utelatt` (standard) | Feltet mangler | Behold eksisterende verdi. |
| `PatchFelt.Null` | `"felt": null` | Be om nullstilling der Oppgave tillater det. |
| `PatchFelt.Verdi(verdi)` | `"felt": verdi` | Sett ny verdi. |

Serialiseringen bruker annotasjoner på klientens egne typer, uten å endre konsumentens globale `ObjectMapper`. Bare `Utelatt` utelates. Tom streng og `false` beholdes, og nullstilling uttrykkes med `PatchFelt.Null`, ikke `PatchFelt.Verdi(null)`.

Ved `409` må konsumenten ta stilling til konflikten. Klienten henter ikke ny versjon og overskriver automatisk.

## Feil og personopplysninger

Metodene tar en valgfri `String` som `korrelasjonsId`. `OppgaveRestKlient` velger verdien for `X-Correlation-ID` ved hvert metodekall i denne rekkefølgen:

1. Eksplisitt `korrelasjonsId`, hvis den er oppgitt.
2. `MDC.get(MDCConstants.MDC_CALL_ID)`, altså `callId`, hvis konteksten har en verdi.
3. En ny tilfeldig UUID som streng når begge mangler.

Konsumenter som bruker `LogFilter`, trenger ikke å sende korrelasjons-ID ved hvert kall. Interfacet og alternative implementasjoner er uavhengige av MDC. MDC-verdier brukes uten UUID-konvertering, og exceptionens `korrelasjonsId` er samme streng som ble sendt i headeren. En eksplisitt UUID-verdi kan sendes med `uuid.toString()`.

Klienten leser MDC når kallet utføres, ikke når singleton-bean-en opprettes, og skriver ikke til MDC. Konsumenten må selv etablere og videreføre konteksten, også ved trådbytte og i coroutines. Korrelasjons-ID-er skal være tekniske verdier, aldri personopplysninger eller verdier utledet fra dem.

Klientfabrikkens MDC-interceptor setter `Nav-Call-Id` og `X-Request-Id` fra MDC. En eksplisitt overstyring av `X-Correlation-ID` endrer ikke disse headerne eller MDC-konteksten.

HTTP-implementasjonen av de fire metodene bruker en privat `utfør`-metode i `OppgaveRestKlient`. `runCatching` og `fold` skiller responsbehandling fra `håndterThrowable`. Metoden håndterer HTTP-feil, ukjent innholdstype, direkte konverteringsfeil og øvrige exceptions. `Error` kastes videre uendret.

`OppgaveException` inneholder operasjon, feiltype, eventuell HTTP-status og korrelasjons-ID. Original exception bevares som `cause`, med opprinnelig cause-kjede og stacktrace.

Ved HTTP-feil brukes feilresponsens `feilmelding` som `message` når feltet er en ikke-blank JSON-streng. Meldingen bevares uendret, også med linjeskift eller kontrolltegn. Ved tom, ukjent eller ugyldig feilrespons brukes en generell melding med metadata. Parsing av feilresponsen erstatter ikke originalfeilen og legger ikke til en parsingfeil som `suppressed`.

| Feiltype | Betydning |
|---|---|
| `HTTP` | HTTP-feil fra Oppgave eller tokenendepunktet. Status beholdes, også ved `404` og `409`. |
| `TRANSPORT` | Kallet kunne ikke gjennomføres, for eksempel ved timeout. Andre tekniske feil, også mot tokenendepunktet, havner her. |
| `UGYLDIG_RESPONS` | Svaret manglet body eller kunne ikke leses som forventet JSON. |

Klienten skiller ikke mellom tokenfeil og feil hos Oppgave. En HTTP-status i exceptionen kan derfor stamme fra begge endepunktene. `404` oversettes ikke til `null`.

Klienten søker ikke gjennom cause-kjeden for feilklassifisering. Ved en direkte `InterruptedException` bevares trådens interrupt-flagg. Når svaret mangler body, finnes ingen opprinnelig exception å bevare.

**Hele exceptionen er potensielt sensitiv**, inkludert `message`, `cause`, `suppressed`, response body, headere og stacktrace. Feilene kan inneholde personidentifikatorer, Nav-ident eller fritekst som avslører helse, ytelse eller adressebeskyttelse. En generell toppmelding gjør ikke cause-kjeden trygg. HTTP-feil og deres cause kan også komme fra tokeninnhenting.

Biblioteket logger ikke exceptionen automatisk og serialiserer den ikke som et API-svar. Konsumenten må kontrollere håndteringen i feilmiddleware, applikasjonslogger, Sentry/APM og eventuell eksponering mot sluttbrukere. Ikke logg sensitive detaljer ukritisk. Passord, tokens og nøkler skal aldri logges, heller ikke med `secureLogger`.

Bruk `message` for serverens forklaring og `cause` for originalfeilen. Ved HTTP-feil finnes hele feilresponsen i den opprinnelige `RestClientResponseException`. Konsumenten må behandle feilmeldinger som data, ikke som ferdige logglinjer.

## Telemetri og konsumentoppsett

**Ikke aktiver DEBUG eller TRACE for HTTP-klienten.** Spring `DefaultRestClient` skriver request-objektets `toString()` ved DEBUG. De to request-body-klassene skjuler feltinnhold i denne tekstutskriften, men maskeringen beskytter ikke faktisk JSON, URL-er, headere, feil eller trafikk til tokenendepunktet. Å ha en egen `secureLogger` flytter ikke Springs interne logger til denne loggkanalen.

Hold `org.springframework.web.client.DefaultRestClient` på INFO eller strengere. Ikke sett root-loggeren eller bredere Spring- og HTTP-pakker til DEBUG eller TRACE, siden klienten da arver nivået. Eksempel på konsumentoppsett:

```yaml
logging:
  level:
    root: INFO
    org.springframework.web.client: INFO
    org.springframework.web.client.DefaultRestClient: INFO
    org.springframework.http: INFO
```

Konsumenten må også unngå å logge DTO-er eller JSON-payload manuelt i vanlige applikasjonslogger. INFO-nivå alene hindrer ikke slik eksplisitt logging av sensitive opplysninger.

Bruk operasjonsnavn, rutemal, HTTP-status og varighet. Ikke bruk faktisk URL, query-parametere, oppgaveinnhold eller personidentifikatorer som labels eller span-attributter.

V1-søk sender personopplysninger i query. Klienten beholder observasjonsoppsettet til den injiserte `RestClient` uten å overstyre registeret. Et aktivt register kan dermed gi HTTP-metrikker og spans. Standardfabrikken setter ikke et aktivt register selv.

Konsumenten må rense eller deaktivere instrumentering som eksporterer sensitive URL-, query- eller feildetaljer. Dette gjelder Spring-observasjoner, Java-agenten, proxylogger og lavnivålogging i HTTP-transporten, inkludert `url.full`, `url.query`, eldre `http.url`, body-, header- og exception-opptak. Ikke aktiver JDKs sensitive transportlogging med `jdk.httpclient.HttpClient.log`. Trafikk til tokenendepunktet må også beskyttes mot logging av tokens i body. Biblioteket kan ikke garantere konfigurasjonen utenfor egen kontroll.

Konsumentappen må ha Entra-oppsett og eksplisitt Nais outbound-policy til riktig Oppgave-host. Biblioteket oppretter ingen Nais-ressurser. Forretningsmetrikker og alarmer tilhører konsumentappen.

## Verifikasjon

Kjør modulens og avhengighetenes tester fra repoets rot:

```shell
mvn -pl oppgave-klient -am verify
```

Testene er organisert rundt klientens eget ansvar. Alle testmetoder har eksplisitte `// Arrange`-, `// Act`- og `// Assert`-kommentarer.

| Testklasse | Hva den verifiserer |
|---|---|
| `OppgaveKontraktTest` | HTTP-metode, path, query-parametere, headere, JSON-body, PATCH-tilstander og responsmapping. |
| `OppgaveFeilhåndteringTest` | Servermelding, fallback, ugyldig respons, original cause, avbrudd og fravær av automatisk gjentakelse av skrivekall. |
| `OppgaveKlientAutoConfigurationTest` | Klientens beanvalg, factory/scope-kobling, egne overstyringer og OBO-tokenleverandør. |
| `OppgaveKlientPropertiesTest` | Miljø til URL/scope, eksplisitte overstyringer og manglende konfigurasjon. |

HTTP-testene bruker én konkret variant fordi alle tre arver de samme `final`-metodene i `OppgaveRestKlient`. Variantvalget testes i autokonfigurasjonen. Logging og ekstern instrumentering må verifiseres i konsumentappen med dens faktiske konfigurasjon.

Testene mocker HTTP-transporten og tokenfabrikken. De verifiserer ikke faktiske tilganger i Oppgave, tokenisolasjon i Nais eller konsumentens eksterne instrumentering. Kjør også full regresjonsbygging og repoets sikkerhetskontroller før publisering.

Før første bruk i PREPROD må konsumenten verifisere miljø, scope og tokenvariant med syntetiske testdata. Skrivekall må kontrolleres mot de oppgavene som faktisk ble opprettet eller endret.

## Rollback

Hvis en konsument må rulle tilbake, må den gå tilbake til tidligere bibliotekversjon og tidligere kallsti. Rollback av biblioteket angrer ikke oppgaver som allerede er opprettet eller endret i Oppgave.
