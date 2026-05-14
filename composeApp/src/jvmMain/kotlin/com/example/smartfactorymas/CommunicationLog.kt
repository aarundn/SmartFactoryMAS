import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartfactorymas.Error
import com.example.smartfactorymas.LogEvent
import com.example.smartfactorymas.OnPrimaryContainer
import com.example.smartfactorymas.OnSecondaryContainer
import com.example.smartfactorymas.OnSurface
import com.example.smartfactorymas.OnSurfaceVariant
import com.example.smartfactorymas.OutlineVariant
import com.example.smartfactorymas.Primary
import com.example.smartfactorymas.PrimaryContainer
import com.example.smartfactorymas.PrimaryFixed
import com.example.smartfactorymas.Secondary
import com.example.smartfactorymas.SecondaryContainer
import com.example.smartfactorymas.SurfaceBright
import com.example.smartfactorymas.SurfaceContainerLow
import com.example.smartfactorymas.SurfaceContainerLowest
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun CommunicationLog(logs: List<LogEvent>, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()
    LaunchedEffect(logs.size) { if (logs.isNotEmpty()) { delay(100); listState.animateScrollToItem(logs.size - 1) } }

    Column(modifier = modifier
        .background(SurfaceContainerLowest, RoundedCornerShape(12.dp))
        .border(1.dp, OutlineVariant, RoundedCornerShape(12.dp)).fillMaxHeight()) {
        // Header
        Row(modifier = Modifier.fillMaxWidth()
            .background(SurfaceContainerLow, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            .border(1.dp, OutlineVariant, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Terminal, null, tint = OnSurfaceVariant, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("Multi-Agent System Log", color = OnSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(Modifier.weight(1f))
            Text("${logs.size} events", color = OnSurfaceVariant, fontSize = 10.sp)
        }

        LazyColumn(state = listState,
            modifier = Modifier.fillMaxSize().background(SurfaceBright).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(logs) { event ->
                val isMsg = event.msg.contains("M_MESSAGE") || event.msg.contains("I_MESSAGE")
                val isMMsg = event.msg.contains("M_MESSAGE")

                val rowBg = when {
                    isMMsg                    -> Color(0xFFFFF7ED)          // amber tint for M_MESSAGE
                    isMsg                     -> Color(0xFFEFF6FF)          // blue tint for I_MESSAGE
                    event.level == "error"    -> Color(0xFFFFF1F2)
                    event.level == "warn"     -> Color(0xFFFFFBEB)
                    else                      -> Color.Transparent
                }

                val agentCol = when {
                    isMMsg                                       -> Secondary
                    isMsg                                        -> Primary
                    event.agent == "AMC" || event.agent == "SYS" -> Error
                    event.agent == "ASRH"                        -> Secondary
                    event.agent.startsWith("ARH")                -> PrimaryFixed
                    event.agent.startsWith("AMA")                -> Color(0xFF065F46)   // green
                    event.agent.startsWith("AMV")                -> Color(0xFF4338CA)   // indigo
                    else                                         -> Primary
                }

                Row(modifier = Modifier.fillMaxWidth()
                    .background(rowBg, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp)) {
                    // message type badge
                    if (isMsg) {
                        val badgeLabel = if (isMMsg) "M" else "I"
                        val badgeBg    = if (isMMsg) SecondaryContainer else PrimaryContainer
                        val badgeFg    = if (isMMsg) OnSecondaryContainer else OnPrimaryContainer
                        Box(Modifier.background(badgeBg, RoundedCornerShape(3.dp)).padding(horizontal = 4.dp, vertical = 1.dp)) {
                            Text(badgeLabel, color = badgeFg, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                        }
                        Spacer(Modifier.width(6.dp))
                    }
                    Text("[${System.currentTimeMillis().toTime()}]",
                        color = OnSurfaceVariant, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(6.dp))
                    Text("${event.agent}:", color = agentCol, fontWeight = FontWeight.Bold,
                        fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Spacer(Modifier.width(6.dp))
                    Text(event.msg, color = OnSurface, fontSize = 11.sp, fontFamily = FontFamily.Monospace, lineHeight = 16.sp)
                }
            }
        }
    }
}

fun Long.toTime(): String =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm:ss"))