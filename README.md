# Kein Bier vor Vier 🍺

Eine nicht ganz ernst gemeinte Smartwatch-App: Sie zeigt, in welcher Zeitzone es **gerade 16 Uhr** ist – und wo als Nächstes. Irgendwo auf der Welt ist schließlich immer vier.

Inspiriert von den Wanduhren, auf denen statt 1–12 überall nur die 4 steht.

## Stand

- `core/` – reine Kotlin-Logik (Zeitzonen, Sommerzeit, Countdown zur nächsten Runde) mit Tests
- `wear/` – Wear-OS-App (Compose for Wear OS) und Complication „Bier-Uhr“ fürs Zifferblatt
- geplant: später eine watchOS-Version

## Bauen & testen

Voraussetzung: Android Studio (inkl. Android SDK).

```bash
./gradlew :core:test           # Logik testen
./gradlew :wear:assembleDebug  # APK bauen
```

Zum Ausprobieren in Android Studio einen Wear-OS-Emulator anlegen und die Run-Konfiguration `wear` starten.

## Mitwirkende

- Oliver Hörold
- Claude (Anthropic) – Co-Autor, entwickelt mit [Claude Code](https://claude.com/claude-code)

---

Bitte verantwortungsvoll genießen. 🍻
