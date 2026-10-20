package pl.grekom.netmaster.data

/**
 * LTE EARFCN -> Band mapping based on 3GPP TS 36.101.
 * Covers bands used in Poland (T-Mobile and others).
 */
object BandLookup {

    private data class BandDef(
        val band: Int,
        val label: String,          // "B8 (900)"
        val dlRange: IntRange
    )

    private val bands = listOf(
        BandDef(1,  "B1 (2100)",    0..599),
        BandDef(2,  "B2 (1900)",    600..1199),
        BandDef(3,  "B3 (1800)",    1200..1949),
        BandDef(4,  "B4 (AWS)",     1950..2399),
        BandDef(5,  "B5 (850)",     2400..2649),
        BandDef(7,  "B7 (2600)",    2750..3449),
        BandDef(8,  "B8 (900)",     3450..3799),
        BandDef(20, "B20 (800)",    6150..6449),
        BandDef(28, "B28 (700)",    9210..9659),
        BandDef(32, "B32 (1500)",   9770..9869),
        BandDef(38, "B38 (TDD 2600)", 37750..38249),
        BandDef(40, "B40 (TDD 2300)", 38650..39649),
        BandDef(41, "B41 (TDD 2500)", 39650..41589),
    )

    fun lteBandFromEarfcn(earfcn: Int): String {
        if (earfcn <= 0) return "-"
        bands.firstOrNull { earfcn in it.dlRange }?.let { return it.label }
        return "EARFCN $earfcn"
    }

    /**
     * NR-ARFCN -> approximate band for common Polish 5G deployments.
     * T-Mobile Polska 5G: primarily n78 (3500 MHz) and n1 (2100 MHz) refarm.
     */
    fun nrBandFromArfcn(nrArfcn: Int): String {
        if (nrArfcn <= 0) return "-"
        return when (nrArfcn) {
            in 422000..434000 -> "n1 (2100)"
            in 386000..398000 -> "n3 (1800)"
            in 185000..192000 -> "n8 (900)"
            in 151600..160600 -> "n20 (800)"
            in 143400..145600 -> "n28 (700)"
            in 620000..680000 -> "n78 (3500)"
            in 693000..733000 -> "n77 (3700)"
            else -> "NR-ARFCN $nrArfcn"
        }
    }
}
