package pl.grekom.netmaster.data

/**
 * Complete LTE/NR band mapping for Polish deployments.
 * 3GPP TS 36.101 (LTE) and TS 38.101 (NR).
 */
object BandLookup {

    private data class BandDef(val band: Int, val label: String, val dlRange: IntRange)

    private val lteBands = listOf(
        BandDef(1,  "B1 (2100)",  0..599),
        BandDef(2,  "B2 (1900)",  600..1199),
        BandDef(3,  "B3 (1800)",  1200..1949),
        BandDef(4,  "B4 (AWS)",   1950..2399),
        BandDef(5,  "B5 (850)",   2400..2649),
        BandDef(7,  "B7 (2600)",  2750..3449),
        BandDef(8,  "B8 (900)",   3450..3799),
        BandDef(20, "B20 (800)",  6150..6449),
        BandDef(28, "B28 (700)",  9210..9659),
        BandDef(32, "B32 (1500)", 9770..9869),
        BandDef(38, "B38 (2600T)", 37750..38249),
        BandDef(40, "B40 (2300T)", 38650..39649),
        BandDef(41, "B41 (2500T)", 39650..41589),
    )

    fun lteBandNumber(earfcn: Int): Int {
        if (earfcn <= 0) return -1
        return lteBands.firstOrNull { earfcn in it.dlRange }?.band ?: -1
    }

    fun lteBandLabel(earfcn: Int): String {
        if (earfcn <= 0) return "-"
        return lteBands.firstOrNull { earfcn in it.dlRange }?.label ?: "EARFCN $earfcn"
    }

    fun nrBandNumber(nrArfcn: Int): Int = when (nrArfcn) {
        in 422000..434000 -> 1
        in 386000..398000 -> 3
        in 185000..192000 -> 8
        in 151600..160600 -> 20
        in 143400..145600 -> 28
        in 620000..680000 -> 78
        in 693000..733000 -> 77
        else -> -1
    }

    fun nrBandLabel(nrArfcn: Int): String = when (nrArfcn) {
        in 422000..434000 -> "n1 (2100)"
        in 386000..398000 -> "n3 (1800)"
        in 185000..192000 -> "n8 (900)"
        in 151600..160600 -> "n20 (800)"
        in 143400..145600 -> "n28 (700)"
        in 620000..680000 -> "n78 (3500)"
        in 693000..733000 -> "n77 (3700)"
        else -> if (nrArfcn > 0) "NR-ARFCN $nrArfcn" else "-"
    }

    /** Short display name: "LTE 800", "5G 3500", etc. */
    fun shortLabel(tech: String, earfcn: Int): String {
        val band = if (tech.startsWith("5G")) nrBandLabel(earfcn) else lteBandLabel(earfcn)
        // Pull just the frequency from "(900)" portion
        val freq = Regex("\\((\\d+)[A-Z]?\\)").find(band)?.groupValues?.get(1)
        return if (freq != null) {
            val prefix = if (tech.startsWith("5G")) "5G" else "LTE"
            "$prefix $freq"
        } else band
    }
}
