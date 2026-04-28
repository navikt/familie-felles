# sikkerhet-spring-security

`TokenContext`-implementasjon basert på **Spring Security OAuth2 Resource Server**.

## Kom i gang

Legg til avhengigheten i `pom.xml`:

```xml
<dependency>
    <groupId>no.nav.familie.felles</groupId>
    <artifactId>sikkerhet-spring-security</artifactId>
    <version>${familie-felles.version}</version>
</dependency>
```

Importer konfigurasjonsklassen i applikasjonens hovedklasse eller en `@Configuration`-klasse:

```kotlin
@Import(FamilieFellesSpringSecurityKonfigurasjon::class)
@SpringBootApplication
class MinApp
```

Dette registrerer `SpringSecurityTokenContext` som aktiv `TokenContext`-implementasjon.

## Issuermapping

`SpringSecurityTokenContext` identifiserer issueren via `iss`-claimet i JWT-en. Resten av biblioteket bruker kortnavn som `"azuread"` og `"tokenx"`, ikke fulle issuer-URLer.

På NAIS bygges mappingen automatisk fra miljøvariablene som NAIS alltid injiserer:

| Miljøvariabel | Kortnavn |
|---|---|
| `AZURE_OPENID_CONFIG_ISSUER` | `"azuread"` |
| `TOKEN_X_ISSUER` | `"tokenx"` |

Du trenger ikke konfigurere noe ekstra. I ikke-NAIS-miljøer (f.eks. lokalt uten disse variablene) brukes full issuer-URL i kall til `TokenContextHolder`.

## Bruk

```kotlin
val saksbehandler = TokenContextHolder.getClaimAsString("NAVident")
val harAzureToken = TokenContextHolder.hasTokenFor("azuread")
val bearerToken = TokenContextHolder.getBearerToken("azuread")
```

## Relaterte moduler

| Modul | Beskrivelse |
|-------|-------------|
| `sikkerhet` | Felles API – `TokenContext`-grensesnitt og `TokenContextHolder` |
| `sikkerhet-token-support` | Alternativ implementasjon basert på Nav token-support |
