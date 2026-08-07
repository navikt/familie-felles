# tilgangsmaskin-klient

Klient for å sjekke om en saksbehandler har tilgang til én eller flere personer gjennom Tilgangsmaskinen
(applikasjonen `populasjonstilgangskontroll` i namespacet `tilgangsmaskin`).

Tilgangsmaskinen er tiltenkt å erstatte tilgangssjekken som tidligere lå i `familie-integrasjoner`
(`POST /tilgang/v2/personer`).

## Dokumentasjon for tjenesten

| Hva                | Lenke                                                          |
|--------------------|----------------------------------------------------------------|
| Confluence         | https://confluence.adeo.no/x/JhR8JQ                            |
| Swagger (prod)     | https://tilgangsmaskin.intern.nav.no/swagger-ui/index.html      |
| Swagger (dev)      | https://tilgangsmaskin.intern.dev.nav.no/swagger-ui/index.html  |
| Kildekode          | https://github.com/navikt/populasjonstilgangskontroll          |
| Slack              | #team-tilgangsmaskinen-værsågod                                |

## Endepunktet klienten bruker

Klienten bruker kun bulk-endepunktet, også for oppslag på én person:

```
POST {TILGANGSMASKIN_API_URL}/api/v1/bulk/obo
```

`obo` betyr *on-behalf-of*: Tilgangsmaskinen leser hvilken ansatt det spørres på vegne av fra
`NAVident` i tokenet. **Endepunktet krever et OBO-token** — et maskin-til-maskin-token blir avvist av
tjenestens `TokenTypeGuard`. Konsumenten må derfor levere en `RestClient` som veksler inn
saksbehandlerens token.

### Request

Body er en liste med `brukerId` og ønsket regeltype:

```json
[
  { "brukerId": "12345678910", "type": "KJERNE_REGELTYPE" },
  { "brukerId": "10987654321", "type": "KJERNE_REGELTYPE" }
]
```

### Regeltyper

| Regeltype               | Regler som kjøres                                                                       |
|-------------------------|------------------------------------------------------------------------------------------|
| `KJERNE_REGELTYPE`      | Adressebeskyttelse (fortrolig/strengt fortrolig), skjerming (egen ansatt) og habilitet    |
| `OVERSTYRBAR_REGELTYPE` | Kun de overstyrbare reglene: geografisk tilgang, vergemål, avdød, utenlandsk/ukjent bosted |
| `KOMPLETT_REGELTYPE`    | Kjernereglene **pluss** de overstyrbare reglene                                           |

Klienten bruker `KJERNE_REGELTYPE` som default. Merk at avvisningskodene `AVVIST_GEOGRAFISK`,
`AVVIST_VERGEMÅL`, `AVVIST_AVDØD`, `AVVIST_PERSON_UTLAND` og `AVVIST_UKJENT_BOSTED` **kun kan
forekomme med `KOMPLETT_REGELTYPE` eller `OVERSTYRBAR_REGELTYPE`**. Velg regeltype bevisst i
konsumenten.

### Respons

Statuskoden på selve kallet er `207 Multi-Status`. Utfallet per person ligger i `status`:

```json
{
  "ansattId": "Z999999",
  "resultater": [
    { "brukerId": "12345678910", "status": 204 },
    {
      "brukerId": "10987654321",
      "status": 403,
      "detaljer": {
        "title": "AVVIST_SKJERMING",
        "begrunnelse": "Bruker er skjermet",
        "traceId": "0af7651916cd43dd8448eb211c80319c",
        "kanOverstyres": true
      }
    }
  ]
}
```

| `status` | Betydning                          |
|----------|------------------------------------|
| `204`    | Tilgang                            |
| `403`    | Avvist, se `detaljer`              |
| `404`    | Ukjent bruker (ingen tilgang)      |

Klienten mapper dette til `TilgangsmaskinResultat`, der **kun `204` gir `harTilgang = true`**.
`detaljer.title` mappes til `Avvisningskode` via navnelikhet; ukjente koder blir `UKJENT` slik at
klienten tåler at Tilgangsmaskinen innfører nye koder.

## Bruk

Importer konfigurasjonen i app-konfigurasjonen din:

```kotlin
@Import(TilgangsmaskinKlientConfig::class)
class ApplicationConfig
```

Konfigurasjonen krever:

* Miljøvariabelen `TILGANGSMASKIN_API_URL`, f.eks. `http://populasjonstilgangskontroll.tilgangsmaskin`
* En `RestClient`-bønne kvalifisert med navnet i `TilgangsmaskinKlientConfig.TILGANGSMASKIN_OBO_REST_CLIENT`.

```kotlin
@Bean(TilgangsmaskinKlientConfig.TILGANGSMASKIN_OBO_REST_CLIENT)
fun tilgangsmaskinOboRestClient(
    @Value("\${TILGANGSMASKIN_SCOPE}") scope: String,
): RestClient = entraIDRestClientFactory.lagOboRestKlient(scope) { SikkerhetContext.hentJwt()?.tokenValue }
```

Appen må i tillegg ha `outbound`-tilgang til tjenesten i `nais.yaml`:

```yaml
outbound:
  rules:
    - application: populasjonstilgangskontroll
      namespace: tilgangsmaskin
      cluster: prod-gcp # eller dev-gcp
```

Deretter kan `TilgangsmaskinKlient` injiseres:

```kotlin
val resultater = tilgangsmaskinKlient.sjekkTilgangTilPersoner(personIdenter)
```

## Det klienten håndterer for deg

* **Chunking.** Tilgangsmaskinen svarer `413` på mer enn 1000 brukerId-er per kall
  (`MAKS_ANTALL_IDENTER_PER_KALL`). Klienten deler opp og slår sammen resultatene.
* **Validering.** Identene sendes inn som `Set`, så duplikater er utelukket allerede i signaturen.
  Blanke identer filtreres bort — tjenesten svarer `400` på blank `brukerId`. Tomt sett gir tom
  liste tilbake uten kall.
* **Fail-closed.** Svarer ikke Tilgangsmaskinen for en ident, returneres et resultat med
  `harTilgang = false` framfor at identen mangler i svaret. Konsumenten får alltid ett resultat per
  gyldig ident, og kan ikke ved et uhell tolke manglende svar som tilgang.
* **Feil.** Alle feil pakkes i `TilgangsmaskinException`. Meldingen inneholder **kun** statuskode eller
  exception-type, fordi responsbodyen kan inneholde personidenter og ofte blir eksponert videre av
  konsumenten. Detaljene ligger i `cause`, og `httpStatus` er satt når feilen kom fra et HTTP-svar,
  slik at konsumenten kan skille klientfeil (4xx) fra serverfeil (5xx).

Klienten logger ikke selv — den vet ikke om konsumenten har secureLog. Eneste unntak er en `WARN` ved
ukjent avvisningskode; den inneholder kun koden, aldri personidenter. Konsumenten er ansvarlig for å
logge avvisninger, og bør ta med `traceId`.
