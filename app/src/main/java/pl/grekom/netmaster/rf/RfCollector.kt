package pl.grekom.netmaster.rf

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.telephony.CellInfo
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellInfoWcdma
import android.telephony.CellSignalStrengthNr
import android.telephony.TelephonyManager
import pl.grekom.netmaster.data.BandLookup
import pl.grekom.netmaster.data.NeighborCell
import pl.grekom.netmaster.data.RfSample

class RfCollector(private val context: Context) {

    private val tm: TelephonyManager =
        context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

    data class Snapshot(val serving: RfSample, val neighbors: List<NeighborCell>)

    @SuppressLint("MissingPermission")
    fun snapshot(): Snapshot {
        val operator = try { tm.networkOperatorName ?: "-" } catch (_: Exception) { "-" }
        val mccMnc = try { tm.networkOperator ?: "-" } catch (_: Exception) { "-" }

        val cells: List<CellInfo> = try {
            tm.allCellInfo ?: emptyList()
        } catch (_: Exception) { emptyList() }

        if (cells.isEmpty()) return Snapshot(RfSample(operator = operator, mccMnc = mccMnc), emptyList())

        val serving = cells.firstOrNull { it.isRegistered } ?: cells.first()
        val servingSample = when (serving) {
            is CellInfoLte -> fromLte(serving, operator, mccMnc)
            is CellInfoNr -> fromNr(serving, operator, mccMnc)
            is CellInfoWcdma -> fromWcdma(serving, operator, mccMnc)
            is CellInfoGsm -> fromGsm(serving, operator, mccMnc)
            else -> RfSample(operator = operator, mccMnc = mccMnc)
        }

        // Collect neighbor cells (not registered)
        val neighbors = cells.filter { it != serving && !it.isRegistered }
            .mapNotNull { neighborFrom(it) }
            .sortedByDescending { if (it.rsrp == Int.MAX_VALUE) Int.MIN_VALUE else it.rsrp }
            .take(10)

        return Snapshot(servingSample, neighbors)
    }

    private fun fromLte(info: CellInfoLte, op: String, mccMnc: String): RfSample {
        val s = info.cellSignalStrength
        val id = info.cellIdentity
        val earfcn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) id.earfcn else -1
        val bw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val b = id.bandwidth
            if (b == Int.MAX_VALUE || b <= 0) "-" else "${b / 1000} MHz"
        } else "-"
        val ci = id.ci
        val enb = if (ci > 0) (ci.toLong() ushr 8) else -1L
        val sector = if (ci > 0) (ci and 0xFF) else -1
        return RfSample(
            tech = "LTE",
            operator = op,
            mccMnc = mccMnc,
            cellId = ci.toLong(),
            enbId = enb,
            sectorId = sector,
            pci = id.pci,
            tac = id.tac,
            earfcn = earfcn,
            band = BandLookup.lteBandNumber(earfcn),
            bandLabel = BandLookup.lteBandLabel(earfcn),
            bandwidth = bw,
            rsrp = s.rsrp,
            rsrq = s.rsrq,
            sinr = s.rssnr,
            rssi = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) s.rssi else Int.MAX_VALUE,
            cqi = s.cqi
        )
    }

    private fun fromNr(info: CellInfoNr, op: String, mccMnc: String): RfSample {
        val s = info.cellSignalStrength as? CellSignalStrengthNr
        val id = info.cellIdentity as android.telephony.CellIdentityNr
        val nrArfcn = id.nrarfcn
        return RfSample(
            tech = "5G NR",
            operator = op,
            mccMnc = mccMnc,
            cellId = id.nci,
            pci = id.pci,
            tac = id.tac,
            earfcn = nrArfcn,
            band = BandLookup.nrBandNumber(nrArfcn),
            bandLabel = BandLookup.nrBandLabel(nrArfcn),
            rsrp = s?.ssRsrp ?: Int.MAX_VALUE,
            rsrq = s?.ssRsrq ?: Int.MAX_VALUE,
            sinr = s?.ssSinr ?: Int.MAX_VALUE,
            nrAvailable = true,
            nrState = "Connected"
        )
    }

    private fun fromWcdma(info: CellInfoWcdma, op: String, mccMnc: String): RfSample {
        val s = info.cellSignalStrength
        val id = info.cellIdentity
        return RfSample(
            tech = "3G",
            operator = op,
            mccMnc = mccMnc,
            cellId = id.cid.toLong(),
            pci = id.psc,
            tac = id.lac,
            rssi = s.dbm
        )
    }

    private fun fromGsm(info: CellInfoGsm, op: String, mccMnc: String): RfSample {
        val s = info.cellSignalStrength
        val id = info.cellIdentity
        return RfSample(
            tech = "2G",
            operator = op,
            mccMnc = mccMnc,
            cellId = id.cid.toLong(),
            tac = id.lac,
            rssi = s.dbm
        )
    }

    private fun neighborFrom(info: CellInfo): NeighborCell? {
        return when (info) {
            is CellInfoLte -> {
                val s = info.cellSignalStrength
                val id = info.cellIdentity
                val earfcn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) id.earfcn else -1
                NeighborCell(
                    tech = "LTE",
                    pci = id.pci,
                    cellId = id.ci.toLong(),
                    earfcn = earfcn,
                    band = BandLookup.lteBandNumber(earfcn),
                    bandLabel = BandLookup.lteBandLabel(earfcn),
                    rsrp = s.rsrp,
                    rsrq = s.rsrq,
                    sinr = s.rssnr
                )
            }
            is CellInfoNr -> {
                val s = info.cellSignalStrength as? CellSignalStrengthNr
                val id = info.cellIdentity as android.telephony.CellIdentityNr
                NeighborCell(
                    tech = "5G",
                    pci = id.pci,
                    cellId = id.nci,
                    earfcn = id.nrarfcn,
                    band = BandLookup.nrBandNumber(id.nrarfcn),
                    bandLabel = BandLookup.nrBandLabel(id.nrarfcn),
                    rsrp = s?.ssRsrp ?: Int.MAX_VALUE,
                    rsrq = s?.ssRsrq ?: Int.MAX_VALUE,
                    sinr = s?.ssSinr ?: Int.MAX_VALUE
                )
            }
            is CellInfoWcdma -> {
                val id = info.cellIdentity
                NeighborCell(
                    tech = "3G",
                    pci = id.psc,
                    cellId = id.cid.toLong(),
                    rsrp = info.cellSignalStrength.dbm
                )
            }
            else -> null
        }
    }
}
