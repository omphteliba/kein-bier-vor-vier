# Kein Bier vor Vier 🍺 · It's 5 O'Clock Somewhere

Eine nicht ganz ernst gemeinte Smartwatch-App: Sie zeigt, in welcher Zeitzone es **gerade 16 Uhr** ist – und wo als Nächstes. Irgendwo auf der Welt ist schließlich immer vier.

Inspiriert von den Wanduhren, auf denen statt 1–12 überall nur die 4 steht. Die englische Ausgabe folgt dem Spruch *„It's five o'clock somewhere“* und rechnet deshalb mit 17 Uhr.

## Stand

- `core/` – reine Kotlin-Logik (Zeitzonen, Sommerzeit, Countdown zur nächsten Runde, beide Editionen, deutsche und englische Namen) mit Tests
- `common/` – Android-Teile, die Uhr und Handy teilen: Texte (Deutsch/Englisch), Standortabfrage, Formatierung und die Schrift [Aleo](common/fonts/README.md) (SIL Open Font License)
- `phone/` – Android-App fürs Handy: großer „Jetzt“-Bildschirm im Bier-Design, Tipp auf einen Ort öffnet Google Maps, Spruch-Auswahl in den Einstellungen, die Runden der nächsten 12 Stunden und ein Homescreen-Widget, das sich zu jeder neuen Runde aktualisiert
- `wear/` – Wear-OS-App (Compose for Wear OS): Stadt, Land, Flagge, lokales Bier und Entfernung zu dir; auf Deutsch 16 Uhr, sonst Englisch und 17 Uhr. Dazu zwei Complications: „Kein Bier vor Vier“ (16 Uhr) und „5 O'Clock Somewhere“ (17 Uhr)
- `watchface/` – eigenes Zifferblatt im Watch Face Format, in zwei Varianten aus einer Vorlage (`src/main/watchface/watchface.xml`):
  - `vier` – „Kein Bier vor Vier“, zwölfmal die 4
  - `five` – „It's 5 O'Clock Somewhere“, zwölfmal die 5

  Flagge, Stadt und Countdown kommen aus den Complications der App, die also mit installiert sein muss.
- `tools/make_logo.py` – erzeugt das Logo (Zifferblatt mit zwölf 4ern bzw. 5ern, unten Bier) als SVG in [`docs/logo/`](docs/logo/) und als App-Icon für Handy und Uhr
- geplant: später eine watchOS-Version

## Barrierefreiheit

- Textfarben erfüllen WCAG AA: `#595540` (7,0 : 1 auf Creme) und `#6B6756` (5,3 : 1 auf Creme, 4,6 : 1 auf den Karten)
- TalkBack liest Karten als Einheit vor, Großbuchstaben normal, Deko (Mini-Uhr, Symbole) wird übersprungen; Überschriften sind als solche markiert
- Tippflächen mindestens 48 dp; Layout wächst mit großer Schrift (getestet mit 200 %)
- Die Bläschen stehen still, wenn in Android „Animationen entfernen“ aktiv ist
- Das Widget hat eine Vorlesebeschreibung; die Complications der Uhr ebenso

## Bauen & testen

Voraussetzung: Android Studio (inkl. Android SDK).

```bash
./gradlew :core:test                  # Logik testen
./gradlew :phone:assembleDebug        # Handy-App bauen
./gradlew :wear:assembleDebug         # Uhren-App bauen
./gradlew :watchface:assembleDebug    # beide Zifferblätter bauen
```

Zum Ausprobieren in Android Studio einen Wear-OS-Emulator anlegen, die Run-Konfiguration `wear` starten und ein Zifferblatt installieren und aktivieren:

```bash
adb install -r watchface/build/outputs/apk/vier/debug/watchface-vier-debug.apk
adb shell am broadcast -a com.google.android.wearable.app.DEBUG_SURFACE --es operation set-watchface --es watchFaceId de.keinbiervorvier.watchface.vier
```

## Mitwirkende

- Oliver Hörold
- Claude (Anthropic) – Co-Autor, entwickelt mit [Claude Code](https://claude.com/claude-code)

---

Bitte verantwortungsvoll genießen. 🍻
