# Grekom NetMaster

Aplikacja Android do walk-testów RF (LTE / 5G NR / 3G / 2G) dla instalacji DAS i sieci T-Mobile.

## Funkcje

- Odczyt parametrów serving cell: **RSRP, RSRQ, SINR, RSSI, EARFCN, pasmo (B1/B3/B7/B8/B20/B28, n78, n1...), PCI, TAC, Cell ID**
- Automatyczna detekcja pasma na podstawie EARFCN
- Wbudowany speed test (download/upload/ping) przez serwery Cloudflare
- GPS każdego pomiaru
- Zdjęcie z aparatu dołączone do pomiaru
- Pole notatek na każdy punkt
- **Zbiorczy PDF** z całego walk-testu: strona tytułowa z logo Grekom, tabela zbiorcza, strona szczegółowa per punkt

## Jak zbudować APK (bez instalacji Android Studio)

Projekt ma skonfigurowany **GitHub Actions** - APK buduje się automatycznie w chmurze.

### Krok po kroku

1. **Załóż konto na github.com** (darmowe)
2. Kliknij **New repository** → nazwij np. `grekom-netmaster` → **Create**
3. Na stronie repo kliknij **"uploading an existing file"**
4. **Przeciągnij do przeglądarki całą zawartość tego folderu** (nie sam folder — jego zawartość: `app/`, `.github/`, `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties`, `gradle/`, `README.md`)
5. Na dole wpisz np. "initial commit" i kliknij **Commit changes**
6. Przejdź do zakładki **Actions** w swoim repo → zobaczysz uruchomiony build **"Build APK"** (3-5 minut)
7. Po zakończeniu kliknij w build → na dole w sekcji **Artifacts** pobierz **GrekomNetMaster-debug** (ZIP z APK)
8. Rozpakuj i przerzuć APK na telefon (email, Google Drive, kabel USB)
9. Na telefonie włącz **"Instalowanie z nieznanych źródeł"** dla przeglądarki/plików i zainstaluj APK

### Alternatywa: Android Studio

Jeśli masz Android Studio: **File → Open** → wskaż ten folder → **Run** (zielony trójkąt).

## Wymagane uprawnienia

Aplikacja prosi przy pierwszym uruchomieniu o:
- **Lokalizacja (precyzyjna)** — GPS do oznaczenia punktów + wymagane przez Android do dostępu do info o komórce
- **Telefon (READ_PHONE_STATE)** — dostęp do parametrów modemu
- **Aparat** — zdjęcia punktów pomiarowych

## Dodanie prawdziwego logo Grekom

Projekt zawiera placeholder (`app/src/main/res/drawable/grekom_logo.xml`). Aby wstawić prawdziwy logo:

1. Usuń plik `grekom_logo.xml`
2. Wrzuć plik PNG o nazwie **`grekom_logo.png`** (szer. ~600px, przezroczyste tło) do:
   `app/src/main/res/drawable-xxhdpi/grekom_logo.png`
3. Commit → GitHub Actions zbuduje nowy APK z Twoim logo

## Notatki techniczne

- **minSdk 26** (Android 8.0) — niektóre API `CellInfoLte.rssi` i NR wymagają API 29/30
- Dla 5G: appka obsługuje `CellInfoNr` (SSRSRP/SSRSRQ/SSSINR) — działa na większości urządzeń z Android 10+
- Speed test wykonuje: 25MB download + 5MB upload + 4x TCP ping do 1.1.1.1
- PDF jest generowany lokalnie (Android `PdfDocument`), zapisywany w `cacheDir/pdfs/` i udostępniany przez FileProvider

## Struktura

```
app/src/main/java/pl/grekom/netmaster/
├── MainActivity.kt              # entry point + permission handling
├── WalkTestViewModel.kt         # stan walk-testu
├── data/
│   ├── Measurement.kt           # data classes
│   └── BandLookup.kt            # EARFCN → pasmo
├── rf/
│   └── RfCollector.kt           # TelephonyManager wrapper
├── speedtest/
│   └── SpeedTest.kt             # Cloudflare speedtest
├── pdf/
│   └── PdfGenerator.kt          # generowanie PDF
└── ui/
    ├── AppRoot.kt               # Compose UI (ekrany + dialogi)
    └── theme/                   # Material 3 theme
```

## Licencja

Projekt wewnętrzny Grekom Sp. z o.o.
