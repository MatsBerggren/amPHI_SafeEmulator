# SafeEmulator emulates a CSAM Paratus v23 (aka Safe) 
It receives updatets sent by an app in the Evam navigation unit, store this information and makes it available for amPHI mobile client thru their Safe integration. 

## Learn more
This application is based on this https://vishu221b.medium.com/using-openapi-swagger-v3-with-spring-boot-3-8315a47ab015

## Log Analyzer
Open /log-analyzer in the running app to upload an EVAM log file, extract replayable API traffic, inspect the generated request sequence, and download a scenario.json file for later replay.

The analyzer also supports sequential folder import for multiple log files, archives each saved analysis under `data/log-analysis`, and exports complete operation-bounded scenario files under `data/log-analysis/<analysisId>/operations`.

## GUI Mode
Start the app with `--gui` to open the local Operation Distance GUI on port `8765`. The GUI can now list saved operation scenario files, replay them with selectable speed, clear map/history state, and show state transitions with named, color-coded flags.

## Wiki Draft
A Wiki.js-ready update draft for the latest changes is available in `documentation/wikijs-amphi-safeemulator-update-2026-03.md`.
