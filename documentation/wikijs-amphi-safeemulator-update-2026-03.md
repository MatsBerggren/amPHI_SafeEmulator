# amPHI SafeEmulator: wikiuppdatering 2026-03

Detta dokument ar ett Wiki.js-anpassat utkast for senaste forandringarna i amPHI SafeEmulator. Innehallet ar skrivet for att kunna klistras in pa [system/amphi-safeemulator](https://wiki.support4u.se/system/amphi-safeemulator) och dess undersidor.

## Rekommenderade sidor att uppdatera

1. `system/amphi-safeemulator`
2. `system/amphi-safeemulator/forbattringshistorik`
3. `system/amphi-safeemulator/installation-och-drift`
4. Eventuellt en ny sida: `system/amphi-safeemulator/logg-analys-och-replay`

## Fardiga bilder att ladda upp

Foljande bildfiler har skapats och kan laddas upp till Wiki.js:

- `documentation/screenshots/log-analyzer.png`
- `documentation/screenshots/operation-distance-map.png`

Foreslagna bildtexter:

- `log-analyzer.png`: Log Analyzer med uppladdning, sparade analyser och extraherad API-sekvens.
- `operation-distance-map.png`: Operation Distance GUI med val av sparade operationsfiler, replayhastighet och styrknappar.

## Kort sammanfattning for systemsidan

amPHI SafeEmulator har byggts ut med en ny logganalys- och replaykedja for EVAM-loggar.

Nya huvudfunktioner:

- Webbaserad Log Analyzer pa `/log-analyzer`
- Stegvis mappimport dar flera loggfiler lases in i sekvens
- Arkivering av analyser under `data/log-analysis`
- Export av kompletta operationssekvenser till separata `*.scenario.json`-filer
- Replay av sparade scenarion direkt mot emulatorn
- Utbyggd GUI-karta i `--gui`-lage med replaykontroller, filval och statusvisualisering

Syftet ar att forenkla felsokning, validering och uppspelning av verklig EVAM-trafik utan att behov av manuell loggtolkning uppstar.

## Forbattringshistorik

### Log Analyzer

En ny webbvy finns pa `/log-analyzer` i den vanliga Spring Boot-applikationen.

Funktioner:

- uppladdning av enstaka loggfil
- uppladdning av flera filer eller hel mapp
- extraktion av replaybara API-anrop ur EVAM-loggar
- visning av payload, endpoint, tidsordning och extraktionskvalitet
- nedladdning av genererad `scenario.json`
- replay av sparad analys direkt fran webbgranssnittet

Analysresultatet bygger pa befintlig logik i `EvamLogScenarioExtractor`, men ar nu paketerat som en anvandbar webbapplikation i samma projekt.

### Robustare import av stora loggmangder

Tidigare kunde stora eller manga loggfiler ge brutna svar eller ofullstandig JSON i klienten. Detta ar atgardat genom:

- hojda upload-granser till 64 MB
- tydligare JSON-fel for overskriden filstorlek
- robustare frontend-hantering av felaktiga eller tomma svar
- stegvis import via import-sessioner i stallet for en stor multipart-uppladdning

Detta gor att en mapp med loggar kan lasas in fil for fil i korrekt ordning.

### Sparade analyser och operationsexport

Varje analys sparas under:

`data/log-analysis/<analysisId>/`

Typiskt innehall:

- `analysis.json`
- `scenario.json`
- `operations/*.scenario.json`

Operationsfilerna innehaller kompletta handelsesekvenser per operation.

Regler for exporten:

- data fore forsta identifierade operation ignoreras
- endast kompletta operationssegment sparas
- sista sannolikt ofullstandiga operationen ignoreras

Detta gor exporten battre anpassad for replay och isolerad felsokning per arende eller uppdrag.

### Replay-stod

Replay finns nu i tva former:

1. replay av sparad analys via Log Analyzer
2. replay av enskilda operationsfiler via GUI-kartan

Replaymotorn dispatchar idag bland annat till:

- `/api/operations`
- `/api/operationlist`
- `/api/vehiclestate`
- `/api/rakelstate`
- `/api/vehiclestatus`
- `/api/triplocationhistory`
- `/api/methanereport`

### GUI for Operation Distance

`--gui`-laget har byggts ut kraftigt och fungerar nu som ett lokalt webbgranssnitt pa port `8765`.

Nya funktioner:

- lank fran GUI-startsidan till Log Analyzer
- lista med sparade operationsfiler fran arkivet
- mojlighet att valja lokal operationsfil
- replay med hastighet `x1`, `x10`, `x100`, `x1000`
- `Play`, `Pausa`, `Stop` och `Rensa`
- rensning av karta och historik utan omstart

### Forbattrad statusvisualisering i kartan

Kartvyn visar nu endast meningsfulla statustransitioner i vansterspalten.

Dessutom:

- statusandringar markeras med flaggor pa kartan
- statusnamn hamtas fran `VehicleStatus`-data i stallet for att visa bara `stateId`
- flaggor fargkodas beroende pa statusnamn, till exempel kvittering, hamtplats, destination och klart uppdrag

Detta gor GUI:t mer anvandbart vid analys av ett uppdrags verkliga forlopp.

## Installation och drift

### Starta huvudapplikationen

```powershell
./mvnw.cmd spring-boot:run
```

Standardport for huvudapplikationen ar fortsatt `8443` over HTTPS.

### Starta GUI-laget

GUI-laget startas genom att kora applikationen med `--gui`.

Detta startar en lokal HTTP-server pa:

`http://localhost:8765/operation-distance-map.html`

Notera:

- GUI-servern kor i samma process som Spring Boot-applikationen
- efter andringar i den genererade GUI-sidan kravs normalt omstart av processen for att nya HTML-andringar ska synas

### Ladda lagrad repository-data manuellt

Automatisk preload av tidigare JSON-data ar avstangd. Lagrad data kan i stallet laddas manuellt via:

`POST /api/admin/load-stored-data`

Endpointen returnerar antal laddade poster per repository samt total summa.

## Forslag till ny undersida: Logg analys och replay

### Syfte

Ge en enkel arbetsgang for att:

- lasa in verkliga EVAM-loggar
- extrahera replaybar trafik
- spara analyser for senare bruk
- isolera och spela upp enskilda operationer

### Foreslagna bilder pa sidan

Placera garna bilderna i denna ordning:

1. `log-analyzer.png` direkt efter introduktionen av Log Analyzer
2. `operation-distance-map.png` efter avsnittet om replay i GUI-laget

### Rekommenderat arbetssatt

1. Starta applikationen.
2. Oppna `/log-analyzer`.
3. Välj en loggfil eller en hel mapp.
4. Låt analysen slutföras och kontrollera sammanfattningen.
5. Använd exporterade filer under `data/log-analysis/<analysisId>/operations/` för replay eller felsokning.
6. Oppna GUI-laget om du vill spela upp en operationsfil visuellt pa kartan.

### Viktiga utdatafiler

- `analysis.json`: sammanfattning och metadata
- `scenario.json`: full sammanlagd sekvens
- `operations/*.scenario.json`: kompletta sekvenser per operation

### Begransningar och praktiska noteringar

- mycket stora enskilda filer over 64 MB stoppas av upload-skyddet
- replaybarhet beror pa om payload kunnat atervinnas eller infereras ur loggen
- vissa event kan fortfarande markeras som observerade men inte direkt replaybara

## Kort text att klistra in pa systemsidan

Senaste uppdateringarna i amPHI SafeEmulator fokuserar pa logganalys, replay och felsokning. Systemet har nu en webbaserad Log Analyzer pa `/log-analyzer` som kan lasa in enskilda loggfiler eller hela mappar stegvis, extrahera replaybar EVAM-trafik, spara analyser och exportera kompletta operationssekvenser till separata scenariofiler. GUI-laget pa port `8765` har samtidigt byggts ut med replaykontroller, val av sparade operationsfiler, rensning av karta samt tydligare statusvisualisering med flaggor och statusnamn.