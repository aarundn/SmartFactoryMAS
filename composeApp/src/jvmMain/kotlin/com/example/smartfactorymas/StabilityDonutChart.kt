package com.example.smartfactorymas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StabilityDonutChart(stability: BatchStability, modifier: Modifier = Modifier) {
    val stableAngle = (stability.stable / 100f) * 360f
    val improvedAngle = (stability.improved / 100f) * 360f
    val deterioratedAngle = (stability.deteriorated / 100f) * 360f

    val strokeWidth = 30.dp

    Box(modifier = modifier.size(160.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize().padding(strokeWidth / 2)) {
            // رسم الجزء المستقر (أخضر)
            drawArc(
                color = Color(0xFF10B981),
                startAngle = -90f,
                sweepAngle = stableAngle.toFloat(),
                useCenter = false,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Butt),
                size = Size(size.width, size.height)
            )
            // رسم الجزء المتحسن (أزرق)
            drawArc(
                color = Color(0xFF3B82F6),
                startAngle = -90f + stableAngle.toFloat(),
                sweepAngle = improvedAngle.toFloat(),
                useCenter = false,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Butt),
                size = Size(size.width, size.height)
            )
            // رسم الجزء المتدهور (برتقالي/أحمر)
            drawArc(
                color = Color(0xFFF97316),
                startAngle = -90f + stableAngle.toFloat() + improvedAngle.toFloat(),
                sweepAngle = deterioratedAngle.toFloat(),
                useCenter = false,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Butt),
                size = Size(size.width, size.height)
            )
        }

        // النص في المنتصف
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${(stability.stable + stability.improved).toInt()}%",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
            Text("Stability", fontSize = 12.sp, color = Color.Gray)
        }
    }
}