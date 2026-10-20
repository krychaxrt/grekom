package pl.grekom.netmaster.ui.screens

import android.graphics.Color as AndroidColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import pl.grekom.netmaster.MainViewModel
import pl.grekom.netmaster.data.NeighborCell
import pl.grekom.netmaster.data.RfSample
import pl.grekom.netmaster.data.SignalLevel
import pl.grekom.netmaster.location.CellLocator
import pl.grekom.netmaster.ui.theme.signalColor

@Composable
fun LiveScreen(vm: MainViewModel) {
    val rf by vm.liveRf.collectAsState()
    val neighbors by vm.liveNeighbors.collectAsState()
    val station by vm.servingCellLocation.collectAsState()
    val loc by vm.liveLocation.collectAsState()
    val history by vm.rsrpHistory.collectAsState()
    val nearby by vm.nearbyStations.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item { HeroHeader(rf) }
        item { RsrpBigCard(rf, history) }
        item { ParametersGrid(rf) }
        if (station != null && loc != null) {
            item { StationCardWithMiniMap(station!!, loc!!.latitude, loc!!.longitude, rf) }
        } else if (rf.cellId > 0) {
            item { StationHint() }
        }
        if (nearby.isNotEmpty()) {
            item { NearbyStationsStrip(nearby, rf.operator) }
        }
        item {
            Row(
                Modifier.fillMaxWidth().padding(start = 4.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.CellTower, null, modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(6.dp))
                Text("Sąsiedzi (${neighbors.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold)
            }
        }
        if (neighbors.isEmpty()) {
            item {
                Text("Brak widocznych", color = Color.Gray, fontSize = 12.sp,
                    modifier = Modifier.padding(8.dp))
            }
        }
        items(neighbors, key = { "${it.tech}-${it.pci}-${it.earfcn}" }) { n ->
            NeighborRow(n)
        }
    }
}

/* ---------- HERO HEADER ---------- */

@Composable
private fun HeroHeader(rf: RfSample) {
    val transition = rememberInfiniteTransition(label = "live")
    val pulse by transition.animateFloat(
        initialValue = 1f, targetValue = 0.4f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "pulse"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.primary
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(RoundedCornerShape(50))
                        .background(Color(0xFF2ECC71).copy(alpha = pulse)))
                    Spacer(Modifier.width(6.dp))
                    Text("NA ŻYWO", color = Color.White.copy(alpha = 0.75f),
                        fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    rf.operator.ifBlank { "Brak sieci" },
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TechChip(rf.tech)
                    if (rf.bandLabel != "-") {
                        Spacer(Modifier.width(6.dp))
                        BandChip(rf.bandLabel)
                    }
                    if (rf.nrAvailable) {
                        Spacer(Modifier.width(6.dp))
                        Box(Modifier.clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF9B59B6))
                            .padding(horizontal = 6.dp, vertical = 2.dp)) {
                            Text("5G NSA", color = Color.White, fontSize = 10.sp,
                                fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    SignalBars(rf.rsrpLevel())
                }
            }
        }
    }
}

@Composable
private fun TechChip(tech: String) {
    val bg = when (tech) {
        "5G NR" -> Color(0xFF9B59B6)
        "LTE" -> Color(0xFF2ECC71)
        "3G" -> Color(0xFFF39C12)
        "2G" -> Color(0xFFE74C3C)
        else -> Color.Gray
    }
    Box(Modifier.clip(RoundedCornerShape(4.dp)).background(bg)
        .padding(horizontal = 6.dp, vertical = 2.dp)) {
        Text(tech, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun BandChip(label: String) {
    Box(Modifier.clip(RoundedCornerShape(4.dp))
        .background(Color.White.copy(alpha = 0.2f))
        .padding(horizontal = 6.dp, vertical = 2.dp)) {
        Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SignalBars(level: SignalLevel) {
    val threshold = when (level) {
        SignalLevel.EXCELLENT -> 5
        SignalLevel.GOOD -> 4
        SignalLevel.FAIR -> 3
        SignalLevel.POOR -> 2
        SignalLevel.VERY_POOR -> 1
        SignalLevel.UNKNOWN -> 0
    }
    Row(verticalAlignment = Alignment.Bottom) {
        repeat(5) { i ->
            Box(
                Modifier.padding(horizontal = 1.5.dp)
                    .width(4.dp).height((6 + i * 4).dp)
                    .background(
                        if (i < threshold) Color.White else Color.White.copy(alpha = 0.3f),
                        RoundedCornerShape(1.dp)
                    )
            )
        }
    }
}

/* ---------- BIG RSRP CARD WITH MINI CHART ---------- */

@Composable
private fun RsrpBigCard(rf: RfSample, history: List<Int>) {
    val color = signalColor(rf.rsrpLevel())
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("RSRP", fontSize = 11.sp, color = Color.Gray,
                        fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            rf.rsrpStr(),
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Bold,
                            color = color
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("dBm", fontSize = 14.sp, color = Color.Gray,
                            modifier = Modifier.padding(bottom = 8.dp))
                    }
                    Text(signalLevelLabel(rf.rsrpLevel()),
                        fontSize = 13.sp, color = color, fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    MetricMini("RSRQ", rf.rsrqStr(), "dB")
                    Spacer(Modifier.height(6.dp))
                    MetricMini("SNR", rf.sinrStr(), "dB")
                }
            }
            if (history.size >= 2) {
                Spacer(Modifier.height(12.dp))
                RsrpSparkline(history, color)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Min ${history.min()}", fontSize = 10.sp, color = Color.Gray)
                    Text("Śr ${history.average().toInt()}", fontSize = 10.sp, color = Color.Gray)
                    Text("Max ${history.max()}", fontSize = 10.sp, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
private fun MetricMini(label: String, value: String, unit: String) {
    Column(horizontalAlignment = Alignment.End) {
        Text(label, fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(2.dp))
            Text(unit, fontSize = 10.sp, color = Color.Gray,
                modifier = Modifier.padding(bottom = 3.dp))
        }
    }
}

@Composable
private fun RsrpSparkline(history: List<Int>, color: Color) {
    val primary = MaterialTheme.colorScheme.primary
    Canvas(modifier = Modifier.fillMaxWidth().height(50.dp)) {
        if (history.isEmpty()) return@Canvas
        val minV = -120f
        val maxV = -60f
        val range = maxV - minV
        val w = size.width
        val h = size.height

        // Horizontal reference lines
        listOf(-80f, -100f).forEach { v ->
            val y = h - ((v - minV) / range) * h
            drawLine(
                color = Color.Gray.copy(alpha = 0.2f),
                start = Offset(0f, y), end = Offset(w, y),
                strokeWidth = 1f
            )
        }

        val step = if (history.size > 1) w / (history.size - 1) else w
        val path = Path()
        val fillPath = Path()
        history.forEachIndexed { i, v ->
            val x = i * step
            val y = h - ((v.coerceIn(-120, -60) - minV) / range) * h
            if (i == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, h)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }
        fillPath.lineTo(w, h)
        fillPath.close()
        drawPath(fillPath, color.copy(alpha = 0.15f))
        drawPath(path, color, style = Stroke(width = 3f, cap = StrokeCap.Round))

        // Dot at the end
        val lastV = history.last().coerceIn(-120, -60)
        val lastY = h - ((lastV - minV) / range) * h
        drawCircle(color, radius = 5f, center = Offset(w, lastY))
        drawCircle(primary, radius = 2f, center = Offset(w, lastY))
    }
}

private fun signalLevelLabel(level: SignalLevel) = when (level) {
    SignalLevel.EXCELLENT -> "Doskonały"
    SignalLevel.GOOD -> "Dobry"
    SignalLevel.FAIR -> "Umiarkowany"
    SignalLevel.POOR -> "Słaby"
    SignalLevel.VERY_POOR -> "Bardzo słaby"
    SignalLevel.UNKNOWN -> "Brak danych"
}

/* ---------- PARAMETERS GRID ---------- */

@Composable
private fun ParametersGrid(rf: RfSample) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            ParamRow(
                "CI" to (if (rf.cellId > 0) rf.cellId.toString() else "-"),
                "EARFCN" to (if (rf.earfcn > 0) rf.earfcn.toString() else "-"),
                "RSSI" to (if (rf.rssi != Int.MAX_VALUE) "${rf.rssi} dBm" else "-")
            )
            ParamRow(
                "eNb" to (if (rf.enbId > 0) rf.enbId.toString() else "-"),
                "BW" to rf.bandwidth,
                "PCI" to (if (rf.pci >= 0) rf.pci.toString() else "-")
            )
            ParamRow(
                "CID" to (if (rf.sectorId >= 0) rf.sectorId.toString() else "-"),
                "TAC" to (if (rf.tac > 0) rf.tac.toString() else "-"),
                "MCC/MNC" to rf.mccMnc
            )
        }
    }
}

@Composable
private fun ParamRow(a: Pair<String, String>, b: Pair<String, String>, c: Pair<String, String>) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween) {
        ParamCell(a.first, a.second, Modifier.weight(1f))
        ParamCell(b.first, b.second, Modifier.weight(1f))
        ParamCell(c.first, c.second, Modifier.weight(1f))
    }
}

@Composable
private fun ParamCell(label: String, value: String, modifier: Modifier) {
    Column(modifier) {
        Text(label, fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

/* ---------- STATION CARD + MINI MAP ---------- */

@Composable
private fun StationCardWithMiniMap(
    station: pl.grekom.netmaster.data.CellLocation,
    userLat: Double, userLon: Double, rf: RfSample
) {
    val dist = CellLocator.distance(userLat, userLon, station.lat, station.lon).toInt()
    val az = CellLocator.bearing(userLat, userLon, station.lat, station.lon).toInt()
    Card(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CellTower, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text("Stacja bazowa", fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary)
                    Text(station.address ?: String.format("%.5f, %.5f", station.lat, station.lon),
                        fontSize = 12.sp)
                }
            }
            // Mini map
            Box(Modifier.fillMaxWidth().height(140.dp)) {
                AndroidView(
                    factory = { ctx ->
                        MapView(ctx).apply {
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(false)
                            setBuiltInZoomControls(false)
                            controller.setZoom(15.0)
                            controller.setCenter(GeoPoint(
                                (userLat + station.lat) / 2,
                                (userLon + station.lon) / 2
                            ))
                        }
                    },
                    update = { map ->
                        map.overlays.clear()
                        map.overlays.add(Marker(map).apply {
                            position = GeoPoint(userLat, userLon)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        })
                        map.overlays.add(Marker(map).apply {
                            position = GeoPoint(station.lat, station.lon)
                            title = station.address ?: "Stacja"
                        })
                        val line = org.osmdroid.views.overlay.Polyline().apply {
                            setPoints(listOf(GeoPoint(userLat, userLon), GeoPoint(station.lat, station.lon)))
                            outlinePaint.color = AndroidColor.rgb(0, 82, 155)
                            outlinePaint.strokeWidth = 4f
                        }
                        map.overlays.add(line)
                        map.invalidate()
                    }
                )
            }
            Row(Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly) {
                MiniStat("Dystans", "$dist m")
                MiniStat("Azymut", "$az°")
                MiniStat("Źródło", station.source)
            }
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 13.sp,
            color = MaterialTheme.colorScheme.primary)
        Text(label, fontSize = 10.sp, color = Color.Gray)
    }
}

@Composable
private fun StationHint() {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1))) {
        Row(Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Info, null, tint = Color(0xFF856404))
            Spacer(Modifier.width(8.dp))
            Column {
                Text("Lokalizacja stacji",
                    fontWeight = FontWeight.Bold, color = Color(0xFF856404))
                Text("Dodaj klucz OpenCellID w Menu albo wgraj CSV T-Mobile.",
                    fontSize = 11.sp, color = Color(0xFF856404))
            }
        }
    }
}

@Composable
private fun NearbyStationsStrip(
    nearby: List<Pair<pl.grekom.netmaster.location.StationDatabase.Station, Double>>,
    currentOperator: String
) {
    // Group by operator, show counts
    val groups = nearby.groupBy { it.first.operator }
    val currentOp = groups[currentOperator].orEmpty()

    Card(modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4F8))) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationCity, null, tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Stacje UKE w okolicy",
                    fontWeight = FontWeight.Bold, fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.weight(1f))
                Text("${nearby.size} / 1.5 km", fontSize = 11.sp, color = Color.Gray)
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                listOf("T-Mobile", "Orange", "Plus", "Play").forEach { op ->
                    OperatorBadge(op, groups[op]?.size ?: 0, op == currentOperator)
                }
            }
            if (currentOp.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                val closest = currentOp.first()
                Text("Najbliższa ${closest.first.operator}: ${closest.first.fullAddress()} (${closest.second.toInt()} m)",
                    fontSize = 11.sp, color = Color.DarkGray)
            }
        }
    }
}

@Composable
private fun OperatorBadge(op: String, count: Int, highlighted: Boolean) {
    val color = when (op) {
        "T-Mobile" -> Color(0xFFE20074)
        "Orange" -> Color(0xFFFF7900)
        "Plus" -> Color(0xFF00A046)
        "Play" -> Color(0xFF8C32C8)
        else -> Color.Gray
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(32.dp).clip(RoundedCornerShape(50))
                .background(if (count > 0) color else color.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text("$count", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Text(op, fontSize = 10.sp,
            color = if (highlighted) color else Color.Gray,
            fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun NeighborRow(n: NeighborCell) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            TechChip(n.tech)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("PCI ${n.pci}  ·  ${n.bandLabel}",
                    fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Text("EARFCN ${n.earfcn}", fontSize = 10.sp, color = Color.Gray)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${n.rsrpStr()} dBm", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text("RSRQ ${n.rsrqStr()} · SNR ${n.sinrStr()}",
                    fontSize = 10.sp, color = Color.Gray)
            }
        }
    }
}
