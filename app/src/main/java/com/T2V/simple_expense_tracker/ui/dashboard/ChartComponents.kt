package com.T2V.simple_expense_tracker.ui.dashboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import com.T2V.simple_expense_tracker.domain.model.Transaction
import java.util.Locale
import kotlin.math.roundToInt

data class BarChartData(
    val label: String,
    val income: Double,
    val expense: Double,
    val transactions: List<Transaction> = emptyList()
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
    val tooltipBgColor = MaterialTheme.colorScheme.inverseSurface
    val tooltipTextColor = MaterialTheme.colorScheme.inverseOnSurface
    val textMeasurer = rememberTextMeasurer()
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

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
                    if (data.isEmpty()) return@pointerInput
                    detectDragGestures(
                        onDragStart = { offset ->
                            val barAreaWidth = size.width / data.size
                            val rawIndex = (offset.x / barAreaWidth).toInt().coerceIn(0, data.size - 1)
                            selectedIndex = rawIndex
                        },
                        onDrag = { change, _ ->
                            val barAreaWidth = size.width / data.size
                            val rawIndex = (change.position.x / barAreaWidth).toInt().coerceIn(0, data.size - 1)
                            selectedIndex = rawIndex
                        },
                        onDragEnd = { selectedIndex = null },
                        onDragCancel = { selectedIndex = null }
                    )
                }
        ) {
            if (data.isEmpty()) return@Canvas
            val maxValue = when (viewMode) {
                StatsViewMode.INCOME -> data.maxOfOrNull { it.income } ?: 0.0
                StatsViewMode.EXPENSE -> data.maxOfOrNull { it.expense } ?: 0.0
                StatsViewMode.BOTH -> data.maxOf { maxOf(it.income, it.expense) }
            }.coerceAtLeast(1.0)
            val chartHeight = size.height * 0.82f
            val barAreaWidth = size.width / data.size
            val barWidth = (barAreaWidth * 0.28f).coerceAtMost(48f)
            val gap = barWidth * 0.15f

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
                val isSelected = selectedIndex == null || selectedIndex == index
                val alphaMultiplier = if (isSelected) 1f else 0.35f

                when (viewMode) {
                    StatsViewMode.INCOME, StatsViewMode.BOTH -> {
                        val incomeHeight = (item.income / maxValue * chartHeight * animationProgress.value).toFloat()
                        val left = centerX - barWidth - gap / 2
                        drawRoundRect(
                            color = primaryColor.copy(alpha = 0.12f * alphaMultiplier),
                            topLeft = Offset(left, size.height - chartHeight),
                            size = Size(barWidth, chartHeight),
                            cornerRadius = CornerRadius(8f, 8f)
                        )
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    primaryColor.copy(alpha = 1f * alphaMultiplier),
                                    primaryColor.copy(alpha = 0.7f * alphaMultiplier)
                                ),
                                startY = size.height - incomeHeight,
                                endY = size.height
                            ),
                            topLeft = Offset(left, size.height - incomeHeight),
                            size = Size(barWidth, incomeHeight),
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
                            else -> centerX - barWidth / 2
                        }
                        drawRoundRect(
                            color = errorColor.copy(alpha = 0.12f * alphaMultiplier),
                            topLeft = Offset(left, size.height - chartHeight),
                            size = Size(barWidth, chartHeight),
                            cornerRadius = CornerRadius(8f, 8f)
                        )
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    errorColor.copy(alpha = 1f * alphaMultiplier),
                                    errorColor.copy(alpha = 0.7f * alphaMultiplier)
                                ),
                                startY = size.height - expenseHeight,
                                endY = size.height
                            ),
                            topLeft = Offset(left, size.height - expenseHeight),
                            size = Size(barWidth, expenseHeight),
                            cornerRadius = CornerRadius(8f, 8f)
                        )
                    }
                    StatsViewMode.INCOME -> {}
                }
            }

            selectedIndex?.let { index ->
                if (index in data.indices) {
                    val barAreaWidth = size.width / data.size
                    val centerX = barAreaWidth * index + barAreaWidth / 2
                    drawLine(
                        color = tooltipBgColor.copy(alpha = 0.5f),
                        start = Offset(centerX, 0f),
                        end = Offset(centerX, size.height),
                        strokeWidth = 1.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                    )
                    val item = data[index]
                    drawBarChartTooltip(
                        textMeasurer = textMeasurer,
                        item = item,
                        centerX = centerX,
                        tooltipBgColor = tooltipBgColor,
                        tooltipTextColor = tooltipTextColor,
                        primaryColor = primaryColor,
                        errorColor = errorColor,
                        viewMode = viewMode
                    )
                }
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
    val tooltipBgColor = MaterialTheme.colorScheme.inverseSurface
    val tooltipTextColor = MaterialTheme.colorScheme.inverseOnSurface
    val textMeasurer = rememberTextMeasurer()
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

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
                    if (data.isEmpty()) return@pointerInput
                    detectTapGestures(
                        onPress = { offset ->
                            val stepX = size.width / (data.size - 1)
                            selectedIndex = (offset.x / stepX).roundToInt().coerceIn(0, data.size - 1)
                            tryAwaitRelease()
                            selectedIndex = null
                        }
                    )
                }
                .pointerInput(data, viewMode) {
                    if (data.isEmpty()) return@pointerInput
                    detectDragGestures(
                        onDragStart = { offset ->
                            val stepX = size.width / (data.size - 1)
                            selectedIndex = (offset.x / stepX).roundToInt().coerceIn(0, data.size - 1)
                        },
                        onDrag = { change, _ ->
                            val stepX = size.width / (data.size - 1)
                            selectedIndex = (change.position.x / stepX).roundToInt().coerceIn(0, data.size - 1)
                        },
                        onDragEnd = { selectedIndex = null },
                        onDragCancel = { selectedIndex = null }
                    )
                }
        ) {
            if (data.isEmpty()) return@Canvas
            val incomeMax = data.maxOf { it.income }.coerceAtLeast(1.0)
            val expenseMax = data.maxOf { it.expense }.coerceAtLeast(1.0)
            val maxValue = when (viewMode) {
                StatsViewMode.INCOME -> incomeMax
                StatsViewMode.EXPENSE -> expenseMax
                StatsViewMode.BOTH -> maxOf(incomeMax, expenseMax)
            }
            val stepX = size.width / (data.size - 1)
            val chartHeight = size.height * 0.82f

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
                    val isSelected = selectedIndex == i
                    val cx = stepX * i
                    val cy = valueToY(point.income)
                    val alpha = if (selectedIndex == null || isSelected) 1f else 0.35f
                    drawCircle(
                        color = primaryColor.copy(alpha = alpha),
                        radius = if (isSelected) 7f else 5f,
                        center = Offset(cx, cy)
                    )
                    drawCircle(
                        color = onSurface.copy(alpha = alpha),
                        radius = if (isSelected) 3.5f else 2.5f,
                        center = Offset(cx, cy)
                    )
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
                    val isSelected = selectedIndex == i
                    val cx = stepX * i
                    val cy = valueToY(point.expense)
                    val alpha = if (selectedIndex == null || isSelected) 1f else 0.35f
                    drawCircle(
                        color = errorColor.copy(alpha = alpha),
                        radius = if (isSelected) 7f else 5f,
                        center = Offset(cx, cy)
                    )
                    drawCircle(
                        color = onSurface.copy(alpha = alpha),
                        radius = if (isSelected) 3.5f else 2.5f,
                        center = Offset(cx, cy)
                    )
                }
            }

            selectedIndex?.let { index ->
                if (index in data.indices) {
                    val cx = stepX * index
                    drawLine(
                        color = tooltipBgColor.copy(alpha = 0.5f),
                        start = Offset(cx, 0f),
                        end = Offset(cx, size.height),
                        strokeWidth = 1.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                    )
                    val item = data[index]
                    drawLineChartTooltip(
                        textMeasurer = textMeasurer,
                        item = item,
                        centerX = cx,
                        tooltipBgColor = tooltipBgColor,
                        tooltipTextColor = tooltipTextColor,
                        primaryColor = primaryColor,
                        errorColor = errorColor,
                        viewMode = viewMode
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

private fun DrawScope.drawBarChartTooltip(
    textMeasurer: TextMeasurer,
    item: BarChartData,
    centerX: Float,
    tooltipBgColor: Color,
    tooltipTextColor: Color,
    primaryColor: Color,
    errorColor: Color,
    viewMode: StatsViewMode
) {
    val labelStr = item.label
    val incomeStr = "Thu: ${formatCurrency(item.income)}"
    val expenseStr = "Chi: ${formatCurrency(item.expense)}"
    val netBalance = item.income - item.expense
    val netStr = "Chênh lệch: ${if (netBalance >= 0) "+" else "-"}${formatCurrency(kotlin.math.abs(netBalance))}"

    val headerStyle = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = tooltipTextColor)
    val incomeStyle = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, color = primaryColor)
    val expenseStyle = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, color = errorColor)
    val netStyle = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = tooltipTextColor)

    val labelLayout = textMeasurer.measure(labelStr, style = headerStyle)
    val incomeLayout = textMeasurer.measure(incomeStr, style = incomeStyle)
    val expenseLayout = textMeasurer.measure(expenseStr, style = expenseStyle)
    val netLayout = textMeasurer.measure(netStr, style = netStyle)

    val paddingH = 12f
    val paddingV = 10f
    val lineSpacing = 4f

    val maxTextWidth = maxOf(labelLayout.size.width, incomeLayout.size.width, expenseLayout.size.width, netLayout.size.width)
    val contentHeight = labelLayout.size.height + incomeLayout.size.height + expenseLayout.size.height + netLayout.size.height
    val tooltipWidth = maxTextWidth + paddingH * 2
    val tooltipHeight = contentHeight + paddingV * 2 + lineSpacing * 3

    val maxTooltipWidth = size.width * 0.75f
    val finalWidth = minOf(tooltipWidth, maxTooltipWidth)

    var tooltipX = centerX - finalWidth / 2
    tooltipX = tooltipX.coerceIn(4f, size.width - finalWidth - 4f)
    val tooltipY = size.height * 0.82f - tooltipHeight - 12f

    drawRoundRect(
        color = tooltipBgColor,
        topLeft = Offset(tooltipX, tooltipY),
        size = Size(finalWidth, tooltipHeight),
        cornerRadius = CornerRadius(12f, 12f)
    )

    var currentY = tooltipY + paddingV
    val contentX = tooltipX + paddingH

    drawText(textMeasurer, labelStr, topLeft = Offset(contentX, currentY), style = headerStyle)
    currentY += labelLayout.size.height + lineSpacing

    if (viewMode != StatsViewMode.EXPENSE) {
        drawText(textMeasurer, incomeStr, topLeft = Offset(contentX, currentY), style = incomeStyle)
    }
    currentY += incomeLayout.size.height + lineSpacing

    if (viewMode != StatsViewMode.INCOME) {
        drawText(textMeasurer, expenseStr, topLeft = Offset(contentX, currentY), style = expenseStyle)
    }
    currentY += expenseLayout.size.height + lineSpacing

    drawText(textMeasurer, netStr, topLeft = Offset(contentX, currentY), style = netStyle)
}

private fun DrawScope.drawLineChartTooltip(
    textMeasurer: TextMeasurer,
    item: BarChartData,
    centerX: Float,
    tooltipBgColor: Color,
    tooltipTextColor: Color,
    primaryColor: Color,
    errorColor: Color,
    viewMode: StatsViewMode
) {
    val labelStr = item.label
    val incomeStr = "Thu: ${formatCurrency(item.income)}"
    val expenseStr = "Chi: ${formatCurrency(item.expense)}"
    val netBalance = item.income - item.expense
    val netStr = "Chênh lệch: ${if (netBalance >= 0) "+" else "-"}${formatCurrency(kotlin.math.abs(netBalance))}"

    val headerStyle = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = tooltipTextColor)
    val incomeStyle = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, color = primaryColor)
    val expenseStyle = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, color = errorColor)
    val netStyle = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = tooltipTextColor)

    val labelLayout = textMeasurer.measure(labelStr, style = headerStyle)
    val incomeLayout = textMeasurer.measure(incomeStr, style = incomeStyle)
    val expenseLayout = textMeasurer.measure(expenseStr, style = expenseStyle)
    val netLayout = textMeasurer.measure(netStr, style = netStyle)

    val paddingH = 12f
    val paddingV = 10f
    val lineSpacing = 4f

    val maxTextWidth = maxOf(labelLayout.size.width, incomeLayout.size.width, expenseLayout.size.width, netLayout.size.width)
    val contentHeight = labelLayout.size.height + incomeLayout.size.height + expenseLayout.size.height + netLayout.size.height
    val tooltipWidth = maxTextWidth + paddingH * 2
    val tooltipHeight = contentHeight + paddingV * 2 + lineSpacing * 3

    val maxTooltipWidth = size.width * 0.75f
    val finalWidth = minOf(tooltipWidth, maxTooltipWidth)

    var tooltipX = centerX - finalWidth / 2
    tooltipX = tooltipX.coerceIn(4f, size.width - finalWidth - 4f)
    val tooltipY = 4f

    drawRoundRect(
        color = tooltipBgColor,
        topLeft = Offset(tooltipX, tooltipY),
        size = Size(finalWidth, tooltipHeight),
        cornerRadius = CornerRadius(12f, 12f)
    )

    var currentY = tooltipY + paddingV
    val contentX = tooltipX + paddingH

    drawText(textMeasurer, labelStr, topLeft = Offset(contentX, currentY), style = headerStyle)
    currentY += labelLayout.size.height + lineSpacing

    if (viewMode != StatsViewMode.EXPENSE) {
        drawText(textMeasurer, incomeStr, topLeft = Offset(contentX, currentY), style = incomeStyle)
    }
    currentY += incomeLayout.size.height + lineSpacing

    if (viewMode != StatsViewMode.INCOME) {
        drawText(textMeasurer, expenseStr, topLeft = Offset(contentX, currentY), style = expenseStyle)
    }
    currentY += expenseLayout.size.height + lineSpacing

    drawText(textMeasurer, netStr, topLeft = Offset(contentX, currentY), style = netStyle)
}

fun formatCurrency(amount: Double): String {
    val absAmount = kotlin.math.abs(amount)
    return when {
        absAmount >= 1_000_000_000 -> String.format(Locale.US, "%.1fB đ", absAmount / 1_000_000_000)
        absAmount >= 1_000_000 -> String.format(Locale.US, "%.1fM đ", absAmount / 1_000_000)
        absAmount >= 1_000 -> String.format(Locale.US, "%,.0fK đ", absAmount / 1_000)
        else -> String.format(Locale.US, "%,.0f đ", absAmount)
    }
}

fun formatAmount(amount: Double): String {
    val formatted = String.format(Locale.US, "%,.0f", kotlin.math.abs(amount))
    val prefix = if (amount >= 0) "+" else "-"
    return "${prefix}${formatted}đ"
}
