package com.example.smartfactorymas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date

data class ScheduleItem(
    val taskName: String,
    val startTime: Double,
    val endTime: Double
)

fun parseScheduleCsv(file: File): List<ScheduleItem> {
    if (!file.exists()) return emptyList()
    val items = mutableListOf<ScheduleItem>()
    val lines = file.readLines()
    for (i in 1 until lines.size) {
        val parts = lines[i].split(",")
        if (parts.size == 3) {
            val name = parts[0]
            val start = parts[1].toDoubleOrNull() ?: 0.0
            val end = parts[2].toDoubleOrNull() ?: 0.0
            items.add(ScheduleItem(name, start, end))
        }
    }
    return items
}

val DarkBackground = Color(0xFF0F111A)
val CardBackground = Color(0xFF1E212B)
val ConsoleBackground = Color(0xFF0D0F14)
val ColorSOM = Color(0xFF4285F4)
val ColorSOP = Color(0xFFB3261E)
val ColorTextGreen = Color(0xFF4CAF50)
val ColorTextRed = Color(0xFFE53935)
val ColorTextBlue = Color(0xFF64B5F6)
val ColorTextGray = Color(0xFF9E9E9E)
val ColorTextWhite = Color(0xFFE0E0E0)
val ColorGridLine = Color(0xFF2B2F3A)

fun getCurrentTimestamp(): String {
    return SimpleDateFormat("HH:mm:ss").format(Date())
}

@Composable
fun LogLine(line: String) {
    val annotatedString = buildAnnotatedString {
        val parts = line.split(" ", limit = 3)
        if (parts.size >= 3 && parts[0].matches(Regex("\\d{2}:\\d{2}:\\d{2}"))) {
            withStyle(SpanStyle(color = ColorTextGray)) {
                append(parts[0] + " ")
            }
            val tag = parts[1]
            val rest = parts[2]
            val tagColor = when {
                tag.contains("AMS") -> ColorTextRed
                tag.contains("ASRH") -> ColorTextBlue
                tag.contains("SYS") || tag.contains("AMC") || tag.contains("ARH") -> ColorTextGreen
                else -> ColorTextGray
            }
            withStyle(SpanStyle(color = tagColor)) {
                append(tag + " ")
            }
            withStyle(SpanStyle(color = tagColor)) { 
                append(rest)
            }
        } else {
            withStyle(SpanStyle(color = ColorTextGreen)) {
                append(line)
            }
        }
    }
    Text(
        text = annotatedString,
        fontFamily = FontFamily.Monospace,
        fontSize = 13.sp,
        modifier = Modifier.padding(vertical = 2.dp)
    )
}

@Composable
@Preview
fun App() {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = DarkBackground,
            surface = CardBackground,
            primary = ColorSOM,
            onPrimary = Color.White
        )
    ) {
        var terminalLogs by remember { mutableStateOf(listOf("${getCurrentTimestamp()} [SYS] MAS Node 04 Initialization Complete.")) }
        var scheduleItems by remember { mutableStateOf(emptyList<ScheduleItem>()) }
        val coroutineScope = rememberCoroutineScope()
        var isRunning by remember { mutableStateOf(false) }

        fun runEngine(strategy: String) {
            if (isRunning) return
            isRunning = true
            // Clear the previous logs and start fresh for the new run
            terminalLogs = listOf("${getCurrentTimestamp()} [SYS] Starting $strategy Strategy...")
            scheduleItems = emptyList()

            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val resourcesDirStr = System.getProperty("compose.application.resources.dir")
                    var currentDir: File
                    val exePath: String
                    
                    if (resourcesDirStr != null) {
                        // Running as packaged application (.exe / .msi)
                        currentDir = File(resourcesDirStr)
                        exePath = File(currentDir, "core_engine.exe").absolutePath
                    } else {
                        // Running in development mode via IDE / Gradle
                        currentDir = File(System.getProperty("user.dir"))
                        if (currentDir.name == "composeApp") {
                            currentDir = currentDir.parentFile
                        }
                        exePath = File(currentDir, "build/core_engine.exe").absolutePath
                    }

                    val process = ProcessBuilder(exePath, strategy)
                        .directory(currentDir)
                        .redirectErrorStream(true)
                        .start()

                    val reader = BufferedReader(InputStreamReader(process.inputStream))
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        val tsLine = "${getCurrentTimestamp()} $line"
                        withContext(Dispatchers.Main) {
                            terminalLogs = terminalLogs + tsLine
                        }
                    }
                    process.waitFor()

                    val csvFile = File(currentDir, "schedule.csv")
                    val items = parseScheduleCsv(csvFile)
                    withContext(Dispatchers.Main) {
                        scheduleItems = items
                        terminalLogs = terminalLogs + "${getCurrentTimestamp()} [SYS] Schedule successfully parsed. Rendering Gantt Chart..."
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        terminalLogs = terminalLogs + "${getCurrentTimestamp()} [SYS ERROR] Could not run engine: ${e.message}"
                    }
                } finally {
                    withContext(Dispatchers.Main) {
                        isRunning = false
                    }
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Top Bar
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // System Online Pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(CardBackground, RoundedCornerShape(16.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(ColorTextGreen)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "System: Online",
                            color = ColorTextGreen,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { runEngine("SOM") },
                            enabled = !isRunning,
                            colors = ButtonDefaults.buttonColors(containerColor = ColorSOM),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("▷ Run S.O.M Strategy (Zero Risk)", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { runEngine("SOP") },
                            enabled = !isRunning,
                            colors = ButtonDefaults.buttonColors(containerColor = ColorSOP),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("⚠ Run S.O.P Strategy (Calculated Risk)", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Middle Section: Terminal Console
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(CardBackground, RoundedCornerShape(8.dp))
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    // Console Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF282C34))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "＞_ Live Agent Console",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    // Console Body
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(ConsoleBackground)
                            .padding(12.dp)
                    ) {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(terminalLogs) { line ->
                                LogLine(line)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Section: Gantt Chart
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .background(CardBackground, RoundedCornerShape(8.dp))
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    // Chart Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF282C34))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "▦ Reactive Production Schedule (MAS Injected)",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            LegendItem("Production", ColorSOM)
                            LegendItem("Maintenance", ColorTextRed)
                        }
                    }

                    // Chart Body
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    ) {
                        val canvasWidth = maxWidth
                        val canvasHeight = maxHeight
                        val leftLabelWidth = 100.dp
                        val topLabelHeight = 24.dp
                        
                        val rowNames = scheduleItems.map { it.taskName }.distinct().sorted()
                        val maxTime = scheduleItems.maxOfOrNull { it.endTime }?.coerceAtLeast(60.0) ?: 60.0
                        
                        val graphWidth = canvasWidth - leftLabelWidth - 16.dp
                        val dpPerMinute = if (maxTime > 0) graphWidth / maxTime.toFloat() else 0.dp
                        
                        // Draw Grid Lines (Canvas)
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val pxPerMinute = dpPerMinute.toPx()
                            
                            // X-axis scale lines
                            for (i in 0..maxTime.toInt() step 10) {
                                val x = leftLabelWidth.toPx() + (i * pxPerMinute)
                                drawLine(
                                    color = ColorGridLine,
                                    start = Offset(x, topLabelHeight.toPx()),
                                    end = Offset(x, size.height),
                                    strokeWidth = 1f
                                )
                            }
                            // Y-axis separator line
                            drawLine(
                                color = ColorGridLine,
                                start = Offset(leftLabelWidth.toPx(), 0f),
                                end = Offset(leftLabelWidth.toPx(), size.height),
                                strokeWidth = 1f
                            )
                            // Top separator line
                            drawLine(
                                color = ColorGridLine,
                                start = Offset(0f, topLabelHeight.toPx()),
                                end = Offset(size.width, topLabelHeight.toPx()),
                                strokeWidth = 1f
                            )
                        }

                        // X-axis Text Labels
                        for (i in 0..maxTime.toInt() step 10) {
                            Text(
                                text = "${i}m",
                                color = ColorTextGray,
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .offset(x = leftLabelWidth + (dpPerMinute * i.toFloat()) - 10.dp, y = 0.dp)
                            )
                        }

                        // Y-axis Labels & Bars
                        val rowHeight = 40.dp
                        rowNames.forEachIndexed { index, rowName ->
                            val yPos = topLabelHeight + 8.dp + (rowHeight * index.toFloat())
                            
                            // Y-axis Label
                            Text(
                                text = rowName,
                                color = ColorTextWhite,
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .offset(x = 0.dp, y = yPos + 8.dp)
                                    .width(leftLabelWidth - 8.dp)
                            )
                        }

                        // Schedule Bars
                        scheduleItems.forEach { item ->
                            val rowIndex = rowNames.indexOf(item.taskName)
                            if (rowIndex >= 0) {
                                val xPos = leftLabelWidth + (dpPerMinute * item.startTime.toFloat())
                                val yPos = topLabelHeight + 8.dp + (rowHeight * rowIndex.toFloat())
                                val barWidth = dpPerMinute * (item.endTime - item.startTime).toFloat()
                                
                                val isMaintenance = item.taskName.contains("Maintenance", ignoreCase = true) || item.taskName.contains("Anomaly", ignoreCase = true)
                                val barColor = if (isMaintenance) ColorTextRed else ColorSOM

                                Box(
                                    modifier = Modifier
                                        .offset(x = xPos, y = yPos)
                                        .size(width = barWidth, height = 28.dp)
                                        .background(barColor, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 8.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(
                                        text = item.taskName,
                                        color = if (isMaintenance) Color.White else Color.Black,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LegendItem(text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = text, color = Color.LightGray, fontSize = 12.sp)
    }
}