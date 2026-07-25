package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

data class BarChartData(
    val label: String,
    val value: Float,
    val color: Color
)

@Composable
fun CanvasBarChart(
    data: List<BarChartData>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return

    val maxValue = (data.maxOfOrNull { it.value } ?: 1f).coerceAtLeast(1f)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Submissions by District / Category",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextDarkPrimary
            )
            Spacer(modifier = Modifier.height(16.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                val width = size.width
                val height = size.height - 30.dp.toPx()
                val barWidth = (width / (data.size * 2f))

                data.forEachIndexed { index, item ->
                    val barHeight = (item.value / maxValue) * height
                    val left = (index * 2f + 0.5f) * barWidth
                    val top = height - barHeight

                    // Draw Bar
                    drawRoundRect(
                        color = item.color,
                        topLeft = Offset(left, top),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                    )

                    // Draw Value Text above bar
                    drawContext.canvas.nativeCanvas.drawText(
                        item.value.toInt().toString(),
                        left + barWidth / 2f,
                        top - 8.dp.toPx(),
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.parseColor("#374151")
                            textSize = 28f
                            textAlign = android.graphics.Paint.Align.CENTER
                            isAntiAlias = true
                            typeface = android.graphics.Typeface.DEFAULT_BOLD
                        }
                    )

                    // Draw Label Text below bar
                    val shortLabel = if (item.label.length > 8) item.label.take(7) + "." else item.label
                    drawContext.canvas.nativeCanvas.drawText(
                        shortLabel,
                        left + barWidth / 2f,
                        height + 20.dp.toPx(),
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.parseColor("#6B7280")
                            textSize = 24f
                            textAlign = android.graphics.Paint.Align.CENTER
                            isAntiAlias = true
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CanvasDonutChart(
    completedCount: Int,
    pendingCount: Int,
    modifier: Modifier = Modifier
) {
    val total = (completedCount + pendingCount).coerceAtLeast(1)
    val completedAngle = (completedCount.toFloat() / total.toFloat()) * 360f
    val pendingAngle = 360f - completedAngle

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceCardBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "School Completion Status",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextDarkPrimary
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(130.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 24.dp.toPx()
                        val diameter = size.minDimension - strokeWidth
                        val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)

                        // Pending segment (Light Gray/Orange)
                        drawArc(
                            color = SoftOrangeIcon,
                            startAngle = -90f + completedAngle,
                            sweepAngle = pendingAngle,
                            useCenter = false,
                            topLeft = topLeft,
                            size = Size(diameter, diameter),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
                        )

                        // Completed segment (Forest Green)
                        drawArc(
                            color = ForestDarkGreen,
                            startAngle = -90f,
                            sweepAngle = completedAngle,
                            useCenter = false,
                            topLeft = topLeft,
                            size = Size(diameter, diameter),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val percentage = ((completedCount.toFloat() / total.toFloat()) * 100).toInt()
                        Text(
                            text = "$percentage%",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ForestDarkGreen
                        )
                        Text(
                            text = "Done",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(24.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(ForestDarkGreen, RoundedCornerShape(3.dp))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Submitted: $completedCount Schools",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextDarkPrimary
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(SoftOrangeIcon, RoundedCornerShape(3.dp))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Pending: $pendingCount Schools",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextDarkPrimary
                        )
                    }
                }
            }
        }
    }
}
