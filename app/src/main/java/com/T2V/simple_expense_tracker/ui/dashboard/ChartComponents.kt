package com.T2V.simple_expense_tracker.ui.dashboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

/**
 * Dữ liệu cho một cột trong biểu đồ trụ — lưu cả thu nhập và chi tiêu
 * để vẽ so sánh song song.
 */
data class BarChartData(
    val label: String,
    val income: Double,
    val expense: Double
)

@Composable
fun BarChart(
    data: List<BarChartData>,
    viewMode: StatsViewMode,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    val textMeasurer = rememberTextMeasurer()
    var hoveredBar by remember { mutableStateOf<Pair<Int, Boolean>?>(null) }

    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(data, viewMode) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(1f, animationSpec = tween(800, easing = EaseOutCubic))
    }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .pointerInput(data, viewMode) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val pos = event.changes.first().position
                            val inside = pos.x in 0f..size.width.toFloat() && pos.y in 0f..size.height.toFloat()
                            if (!inside) {
                                hoveredBar = null
                                continue
                            }
                            val barAreaWidth = size.width.toFloat() / data.size
                            val barWidth = (barAreaWidth * 0.28f).coerceAtMost(48f)
                            val gap = barWidth * 0.15f
                            val rawIndex = (pos.x / barAreaWidth).toInt().coerceIn(0, data.lastIndex)
                            val barCenterX = barAreaWidth * rawIndex + barAreaWidth / 2
                            val relX = pos.x - rawIndex * barAreaWidth
                            val isIncome = when (viewMode) {
                                StatsViewMode.BOTH -> relX < barAreaWidth / 2
                                StatsViewMode.INCOME -> true
                                StatsViewMode.EXPENSE -> false
                            }
                            val isOverBar = when (viewMode) {
                                StatsViewMode.BOTH -> {
                                    val incomeLeft = barCenterX - barWidth - gap / 2
                                    val expenseLeft = barCenterX + gap / 2
                                    (relX >= incomeLeft && relX <= incomeLeft + barWidth) ||
                                            (relX >= expenseLeft && relX <= expenseLeft + barWidth)
                                }
                                else -> {
                                    val barLeft = barCenterX - barWidth / 2
                                    relX >= barLeft && relX <= barLeft + barWidth
                                }
                            }
                            hoveredBar = if (isOverBar) (rawIndex to isIncome) else null
                        }
                    }
                }
        ) {
            if (data.isEmpty()) return@Canvas
            val maxValue = when (viewMode) {
                StatsViewMode.INCOME -> data.maxOfOrNull { it.income } ?: 0.0
                StatsViewMode.EXPENSE -> data.maxOfOrNull { it.expense } ?: 0.0
                StatsViewMode.BOTH -> data.maxOf { maxOf(it.income, it.expense) }
            }.coerceAtLeast(1.0)
            val chartHeight = size.height * 0.85f
            val barAreaWidth = size.width / data.size
            val barWidth = (barAreaWidth * 0.28f).coerceAtMost(48f)
            val gap = barWidth * 0.15f

            // Lưới ngang nhẹ
            for (i in 1..3) {
                val y = size.height - (chartHeight * i / 4)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
                )
            }

            data.forEachIndexed { index, item ->
                val centerX = barAreaWidth * index + barAreaWidth / 2
                val isHoveredIncome = hoveredBar == (index to true)
                val isHoveredExpense = hoveredBar == (index to false)

                when (viewMode) {
                    StatsViewMode.INCOME, StatsViewMode.BOTH -> {
                        val incomeHeight = (item.income / maxValue * chartHeight * animationProgress.value).toFloat()
                        val left = centerX - barWidth - gap / 2
                        val hoverBoost = if (isHoveredIncome) 4f else 0f
                        drawRoundRect(
                            color = primaryColor.copy(alpha = 0.12f),
                            topLeft = Offset(left, size.height - chartHeight),
                            size = Size(barWidth, chartHeight + hoverBoost),
                            cornerRadius = CornerRadius(8f, 8f)
                        )
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(primaryColor, primaryColor.copy(alpha = 0.7f)),
                                startY = size.height - incomeHeight,
                                endY = size.height
                            ),
                            topLeft = Offset(left, size.height - incomeHeight - hoverBoost / 2),
                            size = Size(barWidth, incomeHeight + hoverBoost / 2),
                            cornerRadius = CornerRadius(8f, 8f)
                        )
                    }
                    StatsViewMode.EXPENSE -> {}
                }

                when (viewMode) {
                    StatsViewMode.EXPENSE, StatsViewMode.BOTH -> {
                        val expenseHeight = (item.expense / maxValue * chartHeight * animationProgress.value).toFloat()
                        val left = when (viewMode) {
                            StatsViewMode.BOTH -> centerX + gap / 2
                            StatsViewMode.EXPENSE -> centerX - barWidth / 2
                            else -> centerX - barWidth / 2
                        }
                        val hoverBoost = if (isHoveredExpense) 4f else 0f
                        drawRoundRect(
                            color = errorColor.copy(alpha = 0.12f),
                            topLeft = Offset(left, size.height - chartHeight),
                            size = Size(barWidth, chartHeight + hoverBoost),
                            cornerRadius = CornerRadius(8f, 8f)
                        )
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(errorColor, errorColor.copy(alpha = 0.7f)),
                                startY = size.height - expenseHeight,
                                endY = size.height
                            ),
                            topLeft = Offset(left, size.height - expenseHeight - hoverBoost / 2),
                            size = Size(barWidth, expenseHeight + hoverBoost / 2),
                            cornerRadius = CornerRadius(8f, 8f)
                        )
                    }
                    StatsViewMode.INCOME -> {}
                }
            }

            // Tooltip
            hoveredBar?.let { (index, isIncome) ->
                val item = data[index]
                val value = if (isIncome) item.income else item.expense
                val label = if (isIncome) "Thu" else "Chi"
                val text = "$label: ${formatCurrency(kotlin.math.abs(value))}"
                val textLayout = textMeasurer.measure(
                    text,
                    style = TextStyle(color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                )
                val padH = 10.dp.toPx()
                val padV = 6.dp.toPx()
                val rectW = textLayout.size.width + padH * 2
                val rectH = textLayout.size.height + padV * 2
                val barAreaWidth = size.width / data.size
                val barWidth = (barAreaWidth * 0.28f).coerceAtMost(48f)
                val gap = barWidth * 0.15f
                val barCenterX = barAreaWidth * index + barAreaWidth / 2
                val barLeft = when {
                    viewMode == StatsViewMode.BOTH && isIncome -> barCenterX - barWidth - gap / 2
                    viewMode == StatsViewMode.BOTH && !isIncome -> barCenterX + gap / 2
                    else -> barCenterX - barWidth / 2
                }
                val tooltipX = (barLeft + barWidth / 2 - rectW / 2).coerceIn(2f, size.width - rectW - 2f)
                val tooltipY = (size.height * 0.85f - rectH - 8f).coerceAtLeast(2f)
                drawRoundRect(
                    color = if (isIncome) primaryColor else errorColor,
                    topLeft = Offset(tooltipX, tooltipY),
                    size = Size(rectW, rectH),
                    cornerRadius = CornerRadius(6f, 6f)
                )
                drawText(
                    textLayoutResult = textLayout,
                    topLeft = Offset(tooltipX + padH, tooltipY + padV)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            data.forEach { item ->
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = onSurfaceVariant
                )
            }
        }

        when (viewMode) {
            StatsViewMode.BOTH -> {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(primaryColor))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Thu nhập", style = MaterialTheme.typography.labelSmall, color = onSurfaceVariant)
                    Spacer(modifier = Modifier.width(16.dp))
                    Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(errorColor))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Chi tiêu", style = MaterialTheme.typography.labelSmall, color = onSurfaceVariant)
                }
            }
            StatsViewMode.INCOME -> {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(primaryColor))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Chỉ hiển thị Thu nhập", style = MaterialTheme.typography.labelSmall, color = onSurfaceVariant)
                }
            }
            StatsViewMode.EXPENSE -> {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(errorColor))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Chỉ hiển thị Chi tiêu", style = MaterialTheme.typography.labelSmall, color = onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun LineChart(
    data: List<BarChartData>,
    viewMode: StatsViewMode,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    val onSurface = MaterialTheme.colorScheme.onSurface
    val textMeasurer = rememberTextMeasurer()
    var activePoint by remember { mutableStateOf<Pair<Int, Boolean>?>(null) }

    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(data, viewMode) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(1f, animationSpec = tween(1000, easing = EaseOutCubic))
    }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .pointerInput(data, viewMode) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val pos = event.changes.first().position
                            val inside = pos.x in 0f..size.width.toFloat() && pos.y in 0f..size.height.toFloat()
                            if (!inside || data.size < 2) {
                                activePoint = null
                                continue
                            }
                            val stepX = size.width.toFloat() / (data.size - 1)
                            val rawIndex = (pos.x / stepX).toInt().coerceIn(0, data.lastIndex)
                            val distToIdx = kotlin.math.abs(pos.x - stepX * rawIndex)
                            val distToNext = if (rawIndex < data.lastIndex) kotlin.math.abs(pos.x - stepX * (rawIndex + 1)) else Float.MAX_VALUE
                            val idx = if (distToNext < distToIdx && rawIndex < data.lastIndex) rawIndex + 1 else rawIndex
                            val isIncome = when (viewMode) {
                                StatsViewMode.BOTH -> pos.y < size.height / 2
                                StatsViewMode.INCOME -> true
                                StatsViewMode.EXPENSE -> false
                            }
                            activePoint = idx to isIncome
                        }
                    }
                }
        ) {
            if (data.size < 2) return@Canvas
            val incomeMax = data.maxOf { it.income }.coerceAtLeast(1.0)
            val expenseMax = data.maxOf { it.expense }.coerceAtLeast(1.0)
            val maxValue = when (viewMode) {
                StatsViewMode.INCOME -> incomeMax
                StatsViewMode.EXPENSE -> expenseMax
                StatsViewMode.BOTH -> maxOf(incomeMax, expenseMax)
            }
            val stepX = size.width / (data.size - 1)
            val chartHeight = size.height * 0.85f

            fun valueToY(value: Double): Float {
                return (size.height - (value / maxValue * chartHeight * animationProgress.value)).toFloat()
            }

            for (i in 1..3) {
                val y = size.height - (chartHeight * i / 4)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
                )
            }

            if (viewMode != StatsViewMode.EXPENSE) {
                val incomePath = Path().apply {
                    moveTo(0f, size.height)
                    data.forEachIndexed { i, point ->
                        lineTo(stepX * i, valueToY(point.income))
                    }
                    lineTo(size.width, size.height)
                    close()
                }
                drawPath(
                    path = incomePath,
                    brush = Brush.verticalGradient(
                        colors = listOf(primaryColor.copy(alpha = 0.25f), primaryColor.copy(alpha = 0.02f))
                    )
                )
                for (i in 0 until data.size - 1) {
                    drawLine(
                        color = primaryColor,
                        start = Offset(stepX * i, valueToY(data[i].income)),
                        end = Offset(stepX * (i + 1), valueToY(data[i + 1].income)),
                        strokeWidth = 3f,
                        cap = StrokeCap.Round
                    )
                }
                data.forEachIndexed { i, point ->
                    val cx = stepX * i
                    val cy = valueToY(point.income)
                    drawCircle(color = primaryColor, radius = 5f, center = Offset(cx, cy))
                    drawCircle(color = onSurface, radius = 2.5f, center = Offset(cx, cy))
                }
            }

            if (viewMode != StatsViewMode.INCOME) {
                val expensePath = Path().apply {
                    moveTo(0f, size.height)
                    data.forEachIndexed { i, point ->
                        lineTo(stepX * i, valueToY(point.expense))
                    }
                    lineTo(size.width, size.height)
                    close()
                }
                drawPath(
                    path = expensePath,
                    brush = Brush.verticalGradient(
                        colors = listOf(errorColor.copy(alpha = 0.25f), errorColor.copy(alpha = 0.02f))
                    )
                )
                for (i in 0 until data.size - 1) {
                    drawLine(
                        color = errorColor,
                        start = Offset(stepX * i, valueToY(data[i].expense)),
                        end = Offset(stepX * (i + 1), valueToY(data[i + 1].expense)),
                        strokeWidth = 3f,
                        cap = StrokeCap.Round
                    )
                }
                data.forEachIndexed { i, point ->
                    val cx = stepX * i
                    val cy = valueToY(point.expense)
                    drawCircle(color = errorColor, radius = 5f, center = Offset(cx, cy))
                    drawCircle(color = onSurface, radius = 2.5f, center = Offset(cx, cy))
                }
            }

            // Tooltip
            activePoint?.let { (idx, isIncome) ->
                if (idx in data.indices) {
                    val item = data[idx]
                    val value = if (isIncome) item.income else item.expense
                    val label = if (isIncome) "Thu" else "Chi"
                    val text = "$label: ${formatCurrency(kotlin.math.abs(value))}"
                    val textLayout = textMeasurer.measure(
                        text,
                        style = TextStyle(color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    )
                    val padH = 10.dp.toPx()
                    val padV = 6.dp.toPx()
                    val rectW = textLayout.size.width + padH * 2
                    val rectH = textLayout.size.height + padV * 2
                    val tooltipX = (stepX * idx - rectW / 2).coerceIn(2f, size.width - rectW - 2f)
                    val tooltipY = 4f
                    drawRoundRect(
                        color = if (isIncome) primaryColor else errorColor,
                        topLeft = Offset(tooltipX, tooltipY),
                        size = Size(rectW, rectH),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                    drawText(
                        textLayoutResult = textLayout,
                        topLeft = Offset(tooltipX + padH, tooltipY + padV)
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            data.forEach { point ->
                Text(
                    text = point.label,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = onSurfaceVariant
                )
            }
        }

        when (viewMode) {
            StatsViewMode.BOTH -> {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(primaryColor))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Thu nhập", style = MaterialTheme.typography.labelSmall, color = onSurfaceVariant)
                    Spacer(modifier = Modifier.width(16.dp))
                    Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(errorColor))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Chi tiêu", style = MaterialTheme.typography.labelSmall, color = onSurfaceVariant)
                }
            }
            StatsViewMode.INCOME -> {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(primaryColor))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Chỉ hiển thị Thu nhập", style = MaterialTheme.typography.labelSmall, color = onSurfaceVariant)
                }
            }
            StatsViewMode.EXPENSE -> {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(errorColor))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Chỉ hiển thị Chi tiêu", style = MaterialTheme.typography.labelSmall, color = onSurfaceVariant)
                }
            }
        }
    }
}

/** Định dạng số tiền theo kiểu Việt Nam */
fun formatCurrency(amount: Double): String {
    val absAmount = kotlin.math.abs(amount)
    return when {
        absAmount >= 1_000_000_000 -> String.format(Locale.US, "%.1fB đ", absAmount / 1_000_000_000)
        absAmount >= 1_000_000 -> String.format(Locale.US, "%.1fM đ", absAmount / 1_000_000)
        absAmount >= 1_000 -> String.format(Locale.US, "%,.0fK đ", absAmount / 1_000)
        else -> String.format(Locale.US, "%,.0f đ", absAmount)
    }
}

/** Định dạng số tiền hiển thị đầy đủ với dấu +/- */
fun formatAmount(amount: Double): String {
    val formatted = String.format(Locale.US, "%,.0f", kotlin.math.abs(amount))
    val prefix = if (amount >= 0) "+" else "-"
    return "${prefix}${formatted}đ"
}
