# Grekom NetMaster v2

Walk-test RF (LTE / 5G NR / 3G / 2G) z lokalizacją stacji bazowych, dla DAS i sieci T-Mobile.

## Co nowego w v2

- **Niebieski motyw** (brand Grekom)
- **Layout CellMapper-style** - duży nagłówek z operatorem/pasmem, grid parametrów, wskaźnik siły sygnału
- **Lista sąsiednich komórek** (neighbor cells) z PCI, pasmem, RSRP/RSRQ/SINR
- **Lokalizacja stacji bazowej** z OpenCellID + opcja własnego CSV
- **Mapa OpenStreetMap** z pinezką Twojej lokalizacji + markerem stacji + linią do niej
- **Dziennik zmian** (handover komórki, zmiana pasma, zmiana technologii, alarm słabego sygnału)
- **Drive-test** - automatyczne pomiary co X sekund
- **Export CSV** oprócz PDF
- **Kompas + azymut** do serving cell
- Wszystkie pasma: 700/800/900/1800/2100/2600 + n78/n1/n28 (5G)

## Jak zbudować APK

Projekt ma skonfigurowany **GitHub Actions** - APK buduje się automatycznie w chmurze.

### Jeśli masz już repo z v1:

1. W swoim repo kliknij `gekonnetmaster` (albo jak nazwałeś) → **Add file** → **Upload files**
2. Przeciągnij **całą zawartość tego folderu** (wszystko oprócz samego folderu nadrzędnego)
3. GitHub zapyta czy nadpisać istniejące - **tak, nadpisz**
4. **Commit changes** - stare pliki z v1 zostaną zastąpione, Actions zbuduje nowy APK

### Jeśli robisz od zera:

1. github.com → nowe repo (Public, bez README, bez .gitignore)
2. Upload files → przeciągnij **zawartość** folderu z v2 (wszystkie foldery `app`, `.github`, `gradle` + pliki)
3. Commit → zakładka **Actions** → poczekaj 3-5 min
4. Pobierz **GrekomNetMaster-debug** z Artifacts
5. Rozpakuj → zainstaluj APK na telefonie

## OpenCellID klucz API (dla lokalizacji stacji)

Lokalizacja stacji bazowych pokazuje się dopiero gdy wpiszesz darmowy klucz:

1. Rejestracja: https://opencellid.org/register.php
2. Po rejestracji dostaniesz klucz (zaczyna się od `pk.`)
3. W apce: zakładka **Menu** → wklej klucz → **Zapisz**
4. Pierwsza stacja się załaduje w ciągu kilku sekund (1000 zapytań/dzień za darmo)

### Albo własny CSV T-Mobile

Jeśli masz bazę stacji T-Mobile (np. eksport z ich dokumentacji):

Format pliku:
```
cellid,lat,lon,adres
58407953,50.06470,19.94500,Kraków Komasy 3 - wieża Tauron
58407954,50.06500,19.94800,Kraków Komasy 5
```

Wklej do pola "CSV" w Menu → **Zapisz CSV**. Lokalny CSV ma pierwszeństwo przed OpenCellID.

## Wymagane uprawnienia

- **Lokalizacja (precyzyjna)** - GPS + Android wymaga tego do odczytu CellInfo
- **Telefon (READ_PHONE_STATE)** - parametry modemu
- **Aparat** - zdjęcia punktów pomiarowych
- **Powiadomienia** - alarm słabego sygnału

## Minimalne wymagania

- Android 8.0 (API 26) lub nowszy
- 5G NR działa na Android 10+ (API 29)
- GPS do dokładnych pomiarów
- Czujnik kompasu (opcjonalny) do azymutu

## Struktura kodu

```
app/src/main/java/pl/grekom/netmaster/
├── MainActivity.kt
├── MainViewModel.kt
├── data/
│   ├── Measurement.kt       # RfSample, NeighborCell, MeasurementPoint, LogEntry
│   └── BandLookup.kt        # EARFCN → pasmo (LTE + NR)
├── rf/
│   └── RfCollector.kt       # TelephonyManager + CellInfoLte/Nr/Wcdma/Gsm
├── location/
│   ├── CellLocator.kt       # OpenCellID API + CSV lookup + bearing/distance
│   ├── Compass.kt           # TYPE_ROTATION_VECTOR sensor
│   └── Settings.kt          # DataStore preferences
├── speedtest/
│   └── SpeedTest.kt         # Cloudflare speedtest
├── pdf/
│   └── PdfGenerator.kt      # Multi-page PDF z nagłówkami, zdjęciem, neighbors
├── export/
│   └── CsvExporter.kt       # CSV z pełnymi parametrami
└── ui/
    ├── AppRoot.kt           # Tab navigation
    ├── theme/               # Blue theme + signal colors
    └── screens/
        ├── LiveScreen.kt    # CellMapper-style live view
        ├── MapScreen.kt     # OSM map
        ├── LogScreen.kt     # Diary of changes
        ├── WalkTestScreen.kt# Punkty + export
        └── SettingsScreen.kt# OpenCellID key + CSV + alert
```

## Licencja

Projekt wewnętrzny Grekom Sp. z o.o.
