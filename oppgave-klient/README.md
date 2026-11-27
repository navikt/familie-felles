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

En ukjent enumverdi i POST-responsen eller GET etter 409 gir `UGYLDIG_RESPONS`, selv om oppgaven allerede er lagret. Klienten sender ikke en ny POST. Konsumenten må undersøke utfallet og beholde samme UUID ved et nytt forsøk.

Ved 5xx eller timeout må konsumenten selv avgjøre om POST skal prøves igjen, med samme UUID. Klienten utfører ikke automatisk POST-retry eller lager en ny opprettings-UUID.

Tokeninnhentingen kan gjentas én gang ved transportfeil eller HTTP 5xx. Det skjer før Oppgave-kallet og gjentar ikke et kall som allerede er sendt til Oppgave.

Fordeling, ferdigstilling og andre endringer uttrykkes gjennom PATCH. `mappeId` finnes i oppretting, PATCH, søk og respons. Serveren avgjør om verdien kan brukes. Egne mappeoperasjoner, PUT, bulk-PATCH, kodeverksoppslag, hendelser, kommentaroppslag og v2 er utenfor omfanget. v1-PATCH støtter strukturert kommentar.

## Klientvarianter

`OppgaveKlient` er en forseglet basisklasse med den felles HTTP-implementasjonen av de fire operasjonene. Alle tre variantene arver fra denne klassen. Ingen variant er `@Primary`.

Injiser den konkrete klienttypen for å velge auth-variant eksplisitt, uten `@Qualifier`:

```kotlin
class Konsument(
    private val oppgaveKlient: OppgaveOboKlient,
)
```

Basetypen `OppgaveKlient` uttrykker ikke hvilken auth-variant konsumenten bruker, og Spring tillater fortsatt injeksjon av basetypen. Bruk derfor en av de konkrete typene også når bare én klientbean finnes.

I konsumenttester kan du mocke den konkrete klienttypen. Basisklassen er forseglet og kan ikke arves av egne fakes utenfor modulen. Autokonfigurasjonen kan slås av med `familie.felles.klient.oppgave.enabled=false`.

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

Pakken inneholder egne DTO-er. Responsmodellene ignorerer ukjente felter. Status, prioritet, statuskategori, brukertype, identgruppe, metadata-nøkler og sortering bruker enums. Ukjente enumverdier avvises ved lesing. Tema, oppgavetype og behandlingskategorier er fortsatt strenger, siden de følger eksterne kodeverk.

Enumene erstatter tidligere strengtyper i klientens offentlige API. Kall som brukte for eksempel `prioritet = "NORM"`, må bruke `prioritet = Prioritet.NORM`. `SokOppgaverResponse` krever både `antallTreffTotalt` og `oppgaver`.

Request-, søke- og responsmodellene, nestede modeller og `PatchFelt.Verdi` har verdifri `toString()`. Dette beskytter tekstrepresentasjonen, ikke JSON-innhold, felttilgang, URL-er eller ekstern instrumentering.

Datoer bruker `LocalDate`, responstidspunkter bruker `OffsetDateTime`, og tidsgrensene i søk bruker `LocalDateTime` med `ISO_LOCAL_DATE_TIME`. Brøkdeler av sekund beholdes.

Jackson 3s standardmapper normaliserer offset ved lesing til UTC. Tidspunktet beholdes, men opprinnelig offset er ikke garantert.

`PatchOppgaveRequest` krever `versjon`. Øvrige felt bruker `PatchFelt<T>`:

| Verdi i Kotlin | Avtalt JSON | Betydning |
|---|---|---|
| `PatchFelt.Utelatt` (standard) | Feltet mangler | Behold eksisterende verdi. |
| `PatchFelt.Null` | `"felt": null` | Be om nullstilling der Oppgave tillater det. |
| `PatchFelt.Verdi(verdi)` | `"felt": verdi` | Sett ny verdi. |

Serialiseringen bruker annotasjoner på klientens egne typer, uten å endre konsumentens globale `ObjectMapper`. Bare `Utelatt` utelates. Tom streng og `false` beholdes, og nullstilling uttrykkes med `PatchFelt.Null`, ikke `PatchFelt.Verdi(null)`.

Ved `409` må konsumenten ta stilling til konflikten. Klienten henter ikke ny versjon og overskriver automatisk.

### Feltdekning og metadata

Opprettingsmodellen utelater `npidOrFolkeregisterIdent` og aliaset `navPersonIdent` fra serverens v1-kontrakt. Bruk `personident` for personkobling. `aktoerId` er også modellert for serverens eldre kontrakt. PATCH følger serverens dokumenterte PATCH-felt. Søk støtter serverens v1-query-parametere, inkludert mapper, metadata og sortering.

Metadata bruker `Map<MetadataKey, String>`. JSON-nøklene er enum-navn, for eksempel `NORM_DATO` og `SOKNAD_ID`, ikke `normDato` og `søknadId`. Klienten legger ikke til egne formater eller lengdekrav for metadataverdier som serverkontrakten ikke definerer.

**Metadata og søkefilteret `metadataverdi` skal ikke tas i bruk før innhold, datakategorier og behandlingsgrunnlag er avklart med Team Oppgavehåndtering.** Verdiene behandles som potensielt sensitive personopplysninger. Søkefilteret sender dem i URL-en.

Responsen modellerer også `identer`, metadata og serverens øvrige tilleggsfelt. At `kommentar` finnes i responsemodellen, garanterer ikke at serveren fyller feltet i ordinære responser. `personident` er et skrivefelt, ikke et responsefelt.

### Validering hos konsumenten

Klienten kjører ikke requestvalidering. Vil konsumenten validere lokalt, må den utløse de eksisterende Jakarta-feltannotasjonene før klientkallet. Den kan kalle `Validator.validate(request)` og håndtere resultatet, eller bruke `@Validated` på sin egen Spring-bean og `@Valid` på request-parameteren. `@Validated` på requestklassen alene utløser ikke validering.

Ved Spring-metodevalidering må kallet gå gjennom beanens proxy, og Spring må kunne avskjære metoden. Klientens egne metoder er `final` og har ingen proxybasert requestvalidering. Legg derfor `@Validated` på konsumentens egen bean. Kall fra samme bean og objekter opprettet direkte med konstruktøren går utenom Spring-proxyen.

Opprettingsmodellen har `@NotNull`, `@NotBlank` og `@Size` på de aktuelle feltene. `OpprettOppgaveRequest.uuid` er obligatorisk uten standardverdi. Søkemodellen har `@Max(1000)` på `limit`. Annotasjonene kontrollerer ikke organisasjonsnummerets kontrollsiffer, identkombinasjoner eller om søket har minst ett kvalifiserende filter. PATCH-modellen har ingen Jakarta-valideringsregler for `PatchFelt`.

Oppgave-serveren validerer requestene, inkludert særregler, kodeverk, tilgang og oppgaven etter PATCH-merge. Klienten sender verdiene uten å gjenskape disse kontrollene. Serveravslag gir `OppgaveException` med feiltype `HTTP`. Konsumentens lokale valideringsfeil håndteres ikke av klienten og kan inneholde ugyldige feltverdier. Behandle dem som potensielt sensitive.

## Feil og personopplysninger

Metodene tar en valgfri `String` som `korrelasjonsId`. `OppgaveKlient` velger verdien for `X-Correlation-ID` ved hvert metodekall i denne rekkefølgen:

1. Eksplisitt `korrelasjonsId`, hvis den er oppgitt.
2. `MDC.get(MDCConstants.MDC_CALL_ID)`, altså `callId`, hvis konteksten har en verdi.
3. En ny tilfeldig UUID som streng når begge mangler.

Konsumenter som bruker `LogFilter`, trenger ikke å sende korrelasjons-ID ved hvert kall. MDC-verdier brukes uten UUID-konvertering, og exceptionens `korrelasjonsId` er samme streng som ble sendt i headeren. En eksplisitt UUID-verdi kan sendes med `uuid.toString()`.

Klienten leser MDC når kallet utføres, ikke når singleton-bean-en opprettes, og skriver ikke til MDC. Konsumenten må selv etablere og videreføre konteksten, også ved trådbytte og i coroutines. Korrelasjons-ID-er skal være tekniske verdier, aldri personopplysninger eller verdier utledet fra dem.

Klientfabrikkens MDC-interceptor setter `Nav-Call-Id` og `X-Request-Id` fra MDC. En eksplisitt overstyring av `X-Correlation-ID` endrer ikke disse headerne eller MDC-konteksten.

HTTP-implementasjonen av de fire metodene bruker en privat `utfør`-metode i `OppgaveKlient`. `runCatching` og `fold` skiller responsbehandling fra `håndterThrowable`. Metoden håndterer HTTP-feil, ukjent innholdstype, direkte konverteringsfeil og øvrige exceptions. `Error` kastes videre uendret.

`OppgaveException` inneholder operasjon, feiltype, eventuell HTTP-status og korrelasjons-ID. Original exception bevares som `cause` ved HTTP-, transport- og parsingfeil. Konsumentens lokale valideringsfeil håndteres ikke av klienten.

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

**Ikke aktiver DEBUG eller TRACE for HTTP-klienten.** Spring `DefaultRestClient` skriver request-objektets `toString()` ved DEBUG. Modellene skjuler feltinnhold i denne tekstutskriften, men maskeringen beskytter ikke faktisk JSON, URL-er, headere, feil eller trafikk til tokenendepunktet. Å ha en egen `secureLogger` flytter ikke Springs interne logger til denne loggkanalen.

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

Søk registrerer en URI-mal med `{queryN}`-plassholdere i Spring-observasjonen. Verdiene finnes fortsatt i den faktiske request-URL-en. En verdifri mal beskytter derfor ikke attributter som inneholder hele URL-en.

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
| `OppgaveKontraktTest` | HTTP-metode, path, query-parametere, headere, JSON-body, responsmapping og at requests sendes uten lokal validering. |
| `OppgaveModellKontraktTest` | PATCH-tilstander, metadata-nøkler, identaliaser og at `toString()` skjuler feltinnhold. |
| `OppgaveFeltannotasjonerTest` | At POST-feltkrav og `@Max(1000)` på `limit` virker når Bean Validation utløses. |
| `OppgaveFeilhåndteringTest` | Servermelding, fallback, ugyldig respons, original cause, avbrudd og fravær av automatisk gjentakelse av skrivekall. |
| `OppgaveKlientAutoConfigurationTest` | Klientens beanvalg, factory/scope-kobling, egne overstyringer og OBO-tokenleverandør. |
| `OppgaveKlientPropertiesTest` | Miljø til URL/scope, eksplisitte overstyringer og manglende konfigurasjon. |

HTTP-testene bruker én konkret variant fordi alle tre arver de samme `final`-metodene i `OppgaveKlient`. Variantvalget og konkret injeksjon uten `@Qualifier` testes i autokonfigurasjonen. Logging og ekstern instrumentering må verifiseres i konsumentappen med dens faktiske konfigurasjon.

Testene mocker HTTP-transporten og tokenfabrikken. De verifiserer ikke faktiske tilganger i Oppgave, tokenisolasjon i Nais eller konsumentens eksterne instrumentering. Kjør også full regresjonsbygging og repoets sikkerhetskontroller før publisering.

Før første bruk i PREPROD må konsumenten verifisere miljø, scope og tokenvariant med syntetiske testdata. Skrivekall må kontrolleres mot de oppgavene som faktisk ble opprettet eller endret.

## Rollback

Hvis en konsument må rulle tilbake, må den gå tilbake til tidligere bibliotekversjon og tidligere kallsti. Rollback av biblioteket angrer ikke oppgaver som allerede er opprettet eller endret i Oppgave.
