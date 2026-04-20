# sikkerhet-token-support

`TokenContext`-implementasjon basert på **Nav token-support** (`token-validation-spring`).

## Kom i gang

Legg til avhengigheten i `pom.xml`:

```xml
<dependency>
    <groupId>no.nav.familie.felles</groupId>
    <artifactId>sikkerhet-token-support</artifactId>
    <version>${familie-felles.version}</version>
</dependency>
```

Importer deretter konfigurasjonsklassen i applikasjonens hovedklasse eller en `@Configuration`-klasse:

```kotlin
@Import(FamilieFellesNavTokenSupportKonfigurasjon::class)
@SpringBootApplication
class MinApp
```

Dette registrerer `NavTokenSupportTokenContext` som aktiv `TokenContext`-implementasjon og gjør `TokenContextHolder` tilgjengelig i hele applikasjonen.

## Forutsetninger

Nav token-support må være konfigurert i `application.properties` / `application.yaml` med de issuerne applikasjonen skal validere tokens for. Se [nais/token-support](https://github.com/navikt/token-support#required-properties-yaml-or-properties) for detaljer.

Kortnavnene du oppgir der (f.eks. `azuread`, `tokenx`) er de samme som brukes i metodekallene på `TokenContextHolder`.

## Bruk

Etter at konfigurasjonen er på plass brukes `TokenContextHolder` direkte:

```kotlin
val saksbehandler = TokenContextHolder.getClaimAsString("NAVident")
val harAzureToken = TokenContextHolder.hasTokenFor("azuread")
val bearerToken = TokenContextHolder.getBearerToken("azuread")
```

## Relaterte moduler

| Modul | Beskrivelse |
|-------|-------------|
| `sikkerhet` | Felles API – `TokenContext`-grensesnitt og `TokenContextHolder` |
| `sikkerhet-spring-security` | Alternativ implementasjon basert på Spring Security OAuth2 Resource Server |
