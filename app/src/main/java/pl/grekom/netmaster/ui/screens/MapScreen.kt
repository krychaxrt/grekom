package pl.grekom.netmaster.ui.screens

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import pl.grekom.netmaster.MainViewModel

@Composable
fun MapScreen(vm: MainViewModel) {
    val loc by vm.liveLocation.collectAsState()
    val station by vm.servingCellLocation.collectAsState()
    val rf by vm.liveRf.collectAsState()
    val nearby by vm.nearbyStations.collectAsState()

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(14.0)
                    val start = GeoPoint(50.0647, 19.9450) // Kraków default
                    controller.setCenter(start)
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { map ->
                map.overlays.clear()
                // Nearby UKE stations as background markers (color per operator)
                nearby.forEach { (s, _) ->
                    val color = operatorColor(s.operator)
                    val m = Marker(map).apply {
                        position = GeoPoint(s.lat, s.lon)
                        title = "${s.operator}: ${s.fullAddress()}"
                        snippet = "Pasma: ${s.bands.joinToString(", ")} · Azymuty: ${s.azimuths.joinToString(", ")}°"
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    }
                    map.overlays.add(m)
                }
                loc?.let {
                    val you = GeoPoint(it.latitude, it.longitude)
                    val me = Marker(map).apply {
                        position = you
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        title = "Tu jesteś"
                    }
                    map.overlays.add(me)
                    map.controller.setCenter(you)
                }
                station?.let { cl ->
                    val sp = GeoPoint(cl.lat, cl.lon)
                    val m = Marker(map).apply {
                        position = sp
                        title = cl.address ?: "Stacja: ${rf.cellId}"
                        snippet = "${rf.tech} ${rf.bandLabel}"
                    }
                    map.overlays.add(m)
                    loc?.let {
                        val line = Polyline().apply {
                            setPoints(listOf(GeoPoint(it.latitude, it.longitude), sp))
                            outlinePaint.color = AndroidColor.rgb(0, 82, 155)
                            outlinePaint.strokeWidth = 4f
                        }
                        map.overlays.add(line)
                    }
                }
                map.invalidate()
            }
        )

        // Info overlay
        Card(
            modifier = Modifier.align(Alignment.TopCenter).padding(12.dp).fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f))
        ) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${rf.tech} ${rf.bandLabel}", fontWeight = FontWeight.Bold)
                    val subtitle = if (station != null) station?.address ?: "Stacja znana"
                        else "Lokalizacja stacji nieznana (dodaj klucz OpenCellID)"
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
                if (loc != null) Icon(Icons.Default.MyLocation, null, tint = MaterialTheme.colorScheme.primary)
            }
        }

        // Legend bottom-right
        Card(
            modifier = Modifier.align(Alignment.BottomStart).padding(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f))
        ) {
            Column(Modifier.padding(8.dp)) {
                Text("Stacje UKE: ${nearby.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray)
                listOf("T-Mobile" to AndroidColor.rgb(226, 0, 116),
                    "Orange" to AndroidColor.rgb(255, 121, 0),
                    "Plus" to AndroidColor.rgb(0, 160, 70),
                    "Play" to AndroidColor.rgb(140, 50, 200)).forEach { (op, c) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).background(Color(c)))
                        Spacer(Modifier.width(4.dp))
                        Text(op, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

private fun operatorColor(operator: String): Int = when (operator) {
    "T-Mobile" -> AndroidColor.rgb(226, 0, 116)
    "Orange" -> AndroidColor.rgb(255, 121, 0)
    "Plus" -> AndroidColor.rgb(0, 160, 70)
    "Play" -> AndroidColor.rgb(140, 50, 200)
    else -> AndroidColor.rgb(100, 100, 100)
}
