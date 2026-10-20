package pl.grekom.netmaster.location

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

object AppSettings {

    private val KEY_OPENCELLID = stringPreferencesKey("opencellid_key")
    private val KEY_LOCAL_CSV = stringPreferencesKey("local_csv")
    private val KEY_ALERT_RSRP = intPreferencesKey("alert_rsrp_threshold")
    private val KEY_DRIVE_INTERVAL = intPreferencesKey("drive_interval_sec")

    suspend fun openCellIdKey(ctx: Context): String =
        ctx.dataStore.data.map { it[KEY_OPENCELLID] ?: "" }.first()

    suspend fun setOpenCellIdKey(ctx: Context, key: String) {
        ctx.dataStore.edit { it[KEY_OPENCELLID] = key }
    }

    suspend fun localCsv(ctx: Context): String =
        ctx.dataStore.data.map { it[KEY_LOCAL_CSV] ?: "" }.first()

    suspend fun setLocalCsv(ctx: Context, csv: String) {
        ctx.dataStore.edit { it[KEY_LOCAL_CSV] = csv }
    }

    suspend fun alertRsrp(ctx: Context): Int =
        ctx.dataStore.data.map { it[KEY_ALERT_RSRP] ?: -110 }.first()

    suspend fun setAlertRsrp(ctx: Context, v: Int) {
        ctx.dataStore.edit { it[KEY_ALERT_RSRP] = v }
    }

    suspend fun driveInterval(ctx: Context): Int =
        ctx.dataStore.data.map { it[KEY_DRIVE_INTERVAL] ?: 10 }.first()

    suspend fun setDriveInterval(ctx: Context, v: Int) {
        ctx.dataStore.edit { it[KEY_DRIVE_INTERVAL] = v }
    }
}
