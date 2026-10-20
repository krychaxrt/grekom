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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.grekom.netmaster.MainViewModel
import pl.grekom.netmaster.data.LogEntry
import pl.grekom.netmaster.data.LogEventType

@Composable
fun LogScreen(vm: MainViewModel) {
    val log by vm.log.collectAsState()

    if (log.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.History, null, tint = Color.Gray,
                    modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(8.dp))
                Text("Brak zdarzeń", color = Color.Gray)
                Text("Zmiany stacji, pasma i technologii pokażą się tutaj.",
                    color = Color.Gray, fontSize = 11.sp)
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(log, key = { it.timestamp.toString() + it.from + it.to }) { entry -> LogRow(entry) }
    }
}

@Composable
private fun LogRow(e: LogEntry) {
    val (icon: ImageVector, tint: Color, title: String) = when (e.type) {
        LogEventType.CELL_CHANGE -> Triple(Icons.Default.SwapHoriz, Color(0xFF2ECC71), "Zmiana komórki")
        LogEventType.BAND_CHANGE -> Triple(Icons.Default.SwapVert, Color(0xFF3498DB), "Zmiana pasma")
        LogEventType.TECH_CHANGE -> Triple(Icons.Default.Upgrade, Color(0xFF9B59B6), "Zmiana technologii")
        LogEventType.SIGNAL_ALERT -> Triple(Icons.Default.Warning, Color(0xFFE74C3C), "Słaby sygnał")
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(32.dp).clip(RoundedCornerShape(50))
                    .background(tint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) { Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp)) }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                if (e.from.isNotBlank()) {
                    Text("${e.from} → ${e.to}", fontSize = 12.sp, color = Color.Gray)
                } else {
                    Text(e.to, fontSize = 12.sp, color = Color.Gray)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(e.timeStr(), fontSize = 11.sp, color = Color.Gray)
                if (e.rsrp != Int.MAX_VALUE) {
                    Text("RSRP ${e.rsrp}", fontSize = 11.sp, color = Color.Gray)
                }
            }
        }
    }
}
