package pl.grekom.netmaster.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item { ServingHeader(rf) }
        item { ParametersGrid(rf) }
        if (station != null && loc != null) {
            item { StationInfoCard(station!!, loc!!.latitude, loc!!.longitude) }
        } else if (rf.cellId > 0) {
            item { StationLookupHint() }
        }
        item {
            Text(
                "Sąsiednie komórki (${neighbors.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp, start = 4.dp)
            )
        }
        if (neighbors.isEmpty()) {
            item {
                Text("Brak", color = Color.Gray, modifier = Modifier.padding(8.dp))
            }
        }
        items(neighbors, key = { "${it.tech}-${it.pci}-${it.earfcn}" }) { n ->
            NeighborRow(n)
        }
    }
}

@Composable
private fun ServingHeader(rf: RfSample) {
    val bg = signalColor(rf.rsrpLevel())
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    rf.operator.ifBlank { "Brak" },
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(8.dp))
                TechChip(rf.tech)
                Spacer(Modifier.width(8.dp))
                if (rf.bandLabel != "-") {
                    BandChip(rf.bandLabel)
                }
                Spacer(Modifier.weight(1f))
                SignalIndicator(rf.rsrpLevel())
            }
            if (rf.nrAvailable) {
                Spacer(Modifier.height(6.dp))
                Text("5G NSA · ${rf.nrState}",
                    color = Color.White.copy(alpha = 0.9f), fontSize = 11.sp)
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
    Box(Modifier.clip(RoundedCornerShape(4.dp)).background(bg).padding(horizontal = 6.dp, vertical = 2.dp)) {
        Text(tech, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun BandChip(label: String) {
    Box(
        Modifier.clip(RoundedCornerShape(4.dp))
            .background(Color.White.copy(alpha = 0.2f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SignalIndicator(level: SignalLevel) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        repeat(5) { i ->
            val threshold = when (level) {
                SignalLevel.EXCELLENT -> 5
                SignalLevel.GOOD -> 4
                SignalLevel.FAIR -> 3
                SignalLevel.POOR -> 2
                SignalLevel.VERY_POOR -> 1
                SignalLevel.UNKNOWN -> 0
            }
            Box(
                Modifier
                    .padding(horizontal = 1.dp)
                    .width(3.dp)
                    .height((6 + i * 3).dp)
                    .background(
                        if (i < threshold) Color.White else Color.White.copy(alpha = 0.3f),
                        RoundedCornerShape(1.dp)
                    )
            )
        }
    }
}

@Composable
private fun ParametersGrid(rf: RfSample) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            // Row 1: identifiers
            ParamRow(
                "CI" to (if (rf.cellId > 0) rf.cellId.toString() else "-"),
                "EARFCN" to (if (rf.earfcn > 0) rf.earfcn.toString() else "-"),
                "RSSI" to (if (rf.rssi != Int.MAX_VALUE) "${rf.rssi} dBm" else "-")
            )
            ParamRow(
                "eNb" to (if (rf.enbId > 0) rf.enbId.toString() else "-"),
                "BW" to rf.bandwidth,
                "RSRP" to (if (rf.rsrp != Int.MAX_VALUE) "${rf.rsrp} dBm" else "-"),
                rsrpLevel = rf.rsrpLevel()
            )
            ParamRow(
                "CID" to (if (rf.sectorId >= 0) rf.sectorId.toString() else "-"),
                "PCI" to (if (rf.pci >= 0) rf.pci.toString() else "-"),
                "RSRQ" to (if (rf.rsrq != Int.MAX_VALUE) "${rf.rsrq} dB" else "-")
            )
            ParamRow(
                "TAC" to (if (rf.tac > 0) rf.tac.toString() else "-"),
                "MCC/MNC" to rf.mccMnc,
                "SNR" to (if (rf.sinr != Int.MAX_VALUE) "${rf.sinr} dB" else "-")
            )
        }
    }
}

@Composable
private fun ParamRow(
    a: Pair<String, String>,
    b: Pair<String, String>,
    c: Pair<String, String>,
    rsrpLevel: SignalLevel? = null
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        ParamCell(a.first, a.second, Modifier.weight(1f))
        ParamCell(b.first, b.second, Modifier.weight(1f))
        ParamCell(c.first, c.second, Modifier.weight(1f), rsrpLevel)
    }
}

@Composable
private fun ParamCell(label: String, value: String, modifier: Modifier, color: SignalLevel? = null) {
    Column(modifier) {
        Text(label, fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
        Text(
            value,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = color?.let { signalColor(it) } ?: MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun StationInfoCard(
    station: pl.grekom.netmaster.data.CellLocation,
    userLat: Double,
    userLon: Double
) {
    val dist = CellLocator.distance(userLat, userLon, station.lat, station.lon).toInt()
    val az = CellLocator.bearing(userLat, userLon, station.lat, station.lon).toInt()
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        )
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CellTower, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Stacja bazowa",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(6.dp))
            if (station.address != null) {
                Text(station.address, fontSize = 13.sp)
            } else {
                Text(String.format("%.5f, %.5f", station.lat, station.lon), fontSize = 13.sp)
            }
            Text("Dystans: $dist m   ·   Azymut: $az°   ·   ${station.source}",
                fontSize = 11.sp, color = Color.Gray)
        }
    }
}

@Composable
private fun StationLookupHint() {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1))) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Info, null, tint = Color(0xFF856404))
            Spacer(Modifier.width(8.dp))
            Column {
                Text("Lokalizacja stacji", fontWeight = FontWeight.Bold, color = Color(0xFF856404))
                Text("Dodaj klucz OpenCellID w zakładce Menu, albo wgraj własny CSV z lokalizacji T-Mobile.",
                    fontSize = 11.sp, color = Color(0xFF856404))
            }
        }
    }
}

@Composable
private fun NeighborRow(n: NeighborCell) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TechChip(n.tech)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("PCI ${n.pci}  ·  ${n.bandLabel}", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Text("EARFCN ${n.earfcn}", fontSize = 10.sp, color = Color.Gray)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${n.rsrpStr()} dBm", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text("RSRQ ${n.rsrqStr()} · SNR ${n.sinrStr()}", fontSize = 10.sp, color = Color.Gray)
            }
        }
    }
}
