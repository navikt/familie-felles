# Unleash

I hver enkelt applikasjon man skal bruke feature-toggles, blir man nødt til å legge inn `@ComponentScan("no.nav.familie.unleash", ...)` i App-configen. Deretter kan man ta ibruk servicen `UnleashService` som tilbyr `isEnabled()`-metoden.

Hvor vidt man går mot unleash eller ikke kan styres med app-properties:
```
// Default
unleash:
   enabled: true 
   
// Dersom man ønsker at alle toggles skal gi isEnabled = false eller default verdi der det er definert
unleash:
   enabled: false 
```

Pakka krever følgende miljøvariabler:
* UNLEASH_SERVER_API_URL (se [Unleash-doc](https://docs.nais.io/addons/unleash-next/))
* UNLEASH_SERVER_API_TOKEN (se [Unleash-doc](https://docs.nais.io/addons/unleash-next/))
* NAIS_APP_NAME
