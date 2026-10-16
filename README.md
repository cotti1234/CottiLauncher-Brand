# MysticAttack Brand Plugin

Ersetzt die Server-Brand (die Zeile, die im F3-Debug-Screen nach "Sounds: ..."
steht, z.B. "Paper (Velocity)" oder "HugoSMP.net") durch eine eigene,
farbige Nachricht: **MysticAttack.de** in `#501C8C`.

## Warum das funktioniert
Wenn sich ein Spieler mit einem Backend-Server (Paper/Spigot) verbindet,
sendet dieser Server automatisch eine "Brand"-Nachricht über den
Plugin-Message-Channel `minecraft:brand` an den Client. Velocity leitet
diese standardmäßig einfach durch. Das Plugin fängt diese Nachricht ab
(`PluginMessageEvent`), verwirft sie (`ForwardResult.handled()`) und
sendet stattdessen eine eigene Payload mit dem Text "MysticAttack.de"
(inkl. §x-Hex-Farbcode für #501C8C) direkt an den Spieler-Client.

Da ihr ViaVersion einsetzt, spielt es keine Rolle, mit welcher älteren
oder neueren Version sich jemand verbindet — ViaVersion übersetzt die
Plugin-Message ohnehin passend für den jeweiligen Client-Protokoll-Stand.
Ältere Clients (< 1.16) zeigen ggf. nur den reinen Text ohne Farbe an,
das ist ein Client-seitiges Limit, kein Bug im Plugin.

## Build
```bash
mvn clean package
```
Die fertige JAR liegt danach unter `target/MysticAttackBrand.jar`.

## Installation
1. JAR in den `plugins/`-Ordner eures Velocity/voxy-Proxys legen.
2. Proxy neu starten (kein Reload, PluginMessageEvent-Handler brauchen
   den vollständigen Neustart bei Velocity-Forks wie voxy).
3. Fertig — kein Konfigurationsfile nötig, Farbe/Text sind aktuell
   fest im Code hinterlegt (`HEX_COLOR`, `BRAND_TEXT` in
   `MysticAttackBrand.java`), falls ihr das später ändern wollt.

## Kompatibilität mit voxy
Euer Proxy läuft laut Debug-Screen auf `voxy-0.2.18-beta`, einem
Velocity-Fork. Da voxy die Velocity-API implementiert, sollte dieses
Plugin ohne Änderungen funktionieren. Falls voxy den `PluginMessageEvent`
abweichend behandelt oder eigene Brand-Logik vor Plugins ausführt,
meldet euch — dann müsste man das gezielt gegen die voxy-API statt
gegen `velocity-api` bauen.
