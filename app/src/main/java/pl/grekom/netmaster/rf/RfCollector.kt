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
import pl.grekom.netmaster.data.RfSample

class RfCollector(private val context: Context) {

    private val tm: TelephonyManager =
        context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

    @SuppressLint("MissingPermission")
    fun snapshot(): RfSample {
        val operator = try { tm.networkOperatorName ?: "-" } catch (_: Exception) { "-" }
        val mccMnc = try { tm.networkOperator ?: "-" } catch (_: Exception) { "-" }

        val cells: List<CellInfo> = try {
            tm.allCellInfo ?: emptyList()
        } catch (e: SecurityException) {
            return RfSample(operator = operator, mccMnc = mccMnc)
        } catch (_: Exception) {
            emptyList()
        }

        // Prefer registered serving cell
        val serving = cells.firstOrNull { it.isRegistered } ?: cells.firstOrNull()
        serving ?: return RfSample(operator = operator, mccMnc = mccMnc)

        return when (serving) {
            is CellInfoLte -> fromLte(serving, operator, mccMnc)
            is CellInfoNr -> fromNr(serving, operator, mccMnc)
            is CellInfoWcdma -> fromWcdma(serving, operator, mccMnc)
            is CellInfoGsm -> fromGsm(serving, operator, mccMnc)
            else -> RfSample(operator = operator, mccMnc = mccMnc)
        }
    }

    private fun fromLte(info: CellInfoLte, op: String, mccMnc: String): RfSample {
        val s = info.cellSignalStrength
        val id = info.cellIdentity
        val earfcn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) id.earfcn else -1
        return RfSample(
            tech = "LTE",
            operator = op,
            mccMnc = mccMnc,
            cellId = id.ci.toLong(),
            pci = id.pci,
            tac = id.tac,
            earfcn = earfcn,
            bandLabel = BandLookup.lteBandFromEarfcn(earfcn),
            rsrp = s.rsrp,
            rsrq = s.rsrq,
            sinr = s.rssnr,
            rssi = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) s.rssi else Int.MAX_VALUE
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
            bandLabel = BandLookup.nrBandFromArfcn(nrArfcn),
            rsrp = s?.ssRsrp ?: Int.MAX_VALUE,
            rsrq = s?.ssRsrq ?: Int.MAX_VALUE,
            sinr = s?.ssSinr ?: Int.MAX_VALUE
        )
    }

    private fun fromWcdma(info: CellInfoWcdma, op: String, mccMnc: String): RfSample {
        val s = info.cellSignalStrength
        val id = info.cellIdentity
        return RfSample(
            tech = "3G WCDMA",
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
            tech = "2G GSM",
            operator = op,
            mccMnc = mccMnc,
            cellId = id.cid.toLong(),
            tac = id.lac,
            rssi = s.dbm
        )
    }
}
