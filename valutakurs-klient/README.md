# valutakurs-klient
Klient for å hente valutakurser fra European Central Bank (ECB)
## Dokumentasjonen for ECB
https://data.ecb.europa.eu/help/api/overview

### Content negotiation
Tjenesten bruker content negotiation 
`application/vnd.sdmx.genericdata+xml;version=2.1`
for å hente valutakurser.

Man kan få oversikt over content negotiation som støttes på:
https://data.ecb.europa.eu/help/api/content-negotiation

Man bør jevnlig sjekke om det er nye versjoner av content negotiation som støttes og evt oppgradere til nyeste versjon.

### Fremtidige forbedringer
Bytte til å bruke en content negotiation basert på JSON.
### Test
Det er en integrasjonstest som henter valutakurser fra ECB. Denne heter `ValutakursIntegrasjonTest` og ligger i `src/test/java/no/nav/familie/valutakurs/ValutakursIntegrasjonTest.kt`

Testen kan ikke default med bygg mem kan kjøre med profilen `integration`:  
`mvn clean test -Pintegration`
