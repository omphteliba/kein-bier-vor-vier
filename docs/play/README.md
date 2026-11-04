# Google Play – Schritt für Schritt

Für Handy- und Uhren-App unter **einem** Eintrag (`de.irgendwoistimmervier`). Die Zifferblätter kommen später als eigene Einträge.

## 1. Upload-Schlüssel anlegen (einmalig)

Damit signierst du jede Version, die du hochlädst. **Gut aufheben, nie ins Repo!** (Verlierst du ihn, kann Google ihn zurücksetzen – das dauert aber.)

```powershell
& "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe" -genkeypair -v -keystore "$HOME\irgendwo-upload.jks" -alias upload -keyalg RSA -keysize 4096 -validity 36500
```

Es fragt nach einem Passwort (selbst ausdenken, notieren) und ein paar Angaben zu dir – die sieht niemand außer Google.

Dann im Projektordner die Datei `keystore.properties` anlegen (ist per `.gitignore` vom Repo ausgeschlossen):

```properties
storeFile=C:/Users/DEIN_NAME/irgendwo-upload.jks
storePassword=dein-passwort
keyAlias=upload
keyPassword=dein-passwort
```

Danach baut `./gradlew :phone:bundleRelease :wear:bundleRelease` die zwei Dateien zum Hochladen:
- `phone/build/outputs/bundle/release/phone-release.aab`
- `wear/build/outputs/bundle/release/wear-release.aab`

## 2. App in der Play Console anlegen

**Alle Apps → App erstellen**
- App-Name: `Irgendwo ist immer Vier` · Standardsprache: Deutsch · App · Kostenlos
- Erklärungen bestätigen (Richtlinien, US-Exportgesetze)

## 3. Uhr als Formfaktor hinzufügen

**Testen und veröffentlichen → Einstellungen → Erweiterte Einstellungen → Formfaktoren → Wear OS hinzufügen.** Damit bekommt die Uhr eigene Test-Tracks unter demselben Eintrag.

## 4. App-Inhalte (Richtlinien → App-Inhalte)

| Abschnitt | Antwort |
|---|---|
| Datenschutzerklärung | `https://github.com/omphteliba/kein-bier-vor-vier/blob/main/docs/privacy.md` (vorher deine Kontakt-E-Mail eintragen) |
| App-Zugriff | Alle Funktionen ohne besonderen Zugriff verfügbar |
| Werbung | Nein |
| Einstufung des Inhalts | Kategorie „Dienstprogramm/Produktivität/Sonstiges“; Fragen zu Alkohol: **ja, Bezüge auf Alkohol** (Bier, Brauereien); keine Gewalt, kein Glücksspiel, keine Nutzerinteraktion, kein Teilen des Standorts mit anderen Nutzern |
| Zielgruppe | **18 und älter** (nur diese Altersgruppe ankreuzen) |
| Datensicherheit | **Keine Daten erhoben oder geteilt.** Begründung: Der ungefähre Standort wird nur auf dem Gerät verarbeitet und nie übertragen – laut Google gilt das nicht als „Erhebung“. Verschlüsselung: entfällt. Löschung: entfällt. |
| Behörden-App, Finanzen, Gesundheit, Nachrichten | Nein |

## 5. Store-Eintrag

Texte und Grafiken: [listing.md](listing.md). Englisch über **Übersetzungen verwalten → Übersetzung hinzufügen → Englisch (USA)**.
Für Wear OS braucht der Eintrag mindestens einen Uhren-Screenshot (quadratisch).

## 6. Geschlossener Test (Pflicht für neue Konten)

1. **Testen → Geschlossene Tests → Track erstellen** (z. B. „Freunde“), Länder auswählen.
2. **Tester:** eine E-Mail-Liste anlegen (Google-Konten der Tester) – mindestens **12 Personen**.
3. **Release erstellen:** `phone-release.aab` hochladen, Versionshinweis z. B. „Erste Testversion 🍺“.
4. Für die Uhr: Track „Wear OS · Geschlossener Test“, `wear-release.aab` hochladen.
5. Zur Überprüfung einreichen. Nach der Freigabe den **Opt-in-Link** an die Tester schicken: Beitreten, dann App aus dem Play Store installieren.
6. **14 Tage am Stück** mit mindestens 12 Testern – danach kannst du unter **Dashboard** den Zugang zur Produktion beantragen.

## Hinweis zum Testen in Android Studio

Handy- und Uhren-App haben im Store denselben Paketnamen. In **Debug-Builds** heißt die Uhren-App deshalb `de.irgendwoistimmervier.wear` – so kann ein versehentlicher Start auf dem Handy die Handy-App nicht überschreiben. Trotzdem: oben **Gerät und Konfiguration passend wählen** (Nokia + `phone`, Wear-Emulator + `wear`).

## Neue Version hochladen

`versionCode` in `phone/build.gradle.kts` und `wear/build.gradle.kts` erhöhen (Handy und Uhr müssen sich unterscheiden: Handy ungerade, Uhr gerade hat sich bewährt), dann wie in Schritt 1 bauen und im jeweiligen Track ein neues Release anlegen.
