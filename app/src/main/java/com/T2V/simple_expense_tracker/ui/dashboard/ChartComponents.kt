package com.T2V.simple_expense_tracker.ui.dashboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Dữ liệu cho một cột trong biểu đồ trụ — lưu cả thu nhập và chi tiêu
 * để vẽ so sánh song song.
 */
data class BarChartData(
    val label: String,
    val income: Double,
    val expense: Double
)


/**
 * Biểu đồ Trụ (Bar Chart) — hiển thị thu nhập/chi tiêu theo tuần.
 * Mỗi tuần có 2 thanh: xanh (thu) và đỏ (chi).
 */
@Composable
fun BarChart(
    data: List<BarChartData>,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    val textMeasurer = rememberTextMeasurer()

    // Animation cho chiều cao thanh
    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(data) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(1f, animationSpec = tween(800, easing = EaseOutCubic))
    }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            if (data.isEmpty()) return@Canvas
            val maxValue = data.maxOf { maxOf(it.income, it.expense) }.coerceAtLeast(1.0)
            val chartHeight = size.height * 0.85f
            val barAreaWidth = size.width / data.size
            val barWidth = (barAreaWidth * 0.28f).coerceAtMost(48f)
            val gap = barWidth * 0.15f

            // Vẽ lưới ngang nhẹ
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

                // Thanh thu nhập (bên trái)
                val incomeHeight = (item.income / maxValue * chartHeight * animationProgress.value).toFloat()
                drawRoundRect(
                    color = primaryColor.copy(alpha = 0.12f),
                    topLeft = Offset(centerX - barWidth - gap / 2, size.height - chartHeight),
                    size = Size(barWidth, chartHeight),
                    cornerRadius = CornerRadius(8f, 8f)
                )
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(primaryColor, primaryColor.copy(alpha = 0.7f)),
                        startY = size.height - incomeHeight,
                        endY = size.height
                    ),
                    topLeft = Offset(centerX - barWidth - gap / 2, size.height - incomeHeight),
                    size = Size(barWidth, incomeHeight),
                    cornerRadius = CornerRadius(8f, 8f)
                )

                // Thanh chi tiêu (bên phải)
                val expenseHeight = (item.expense / maxValue * chartHeight * animationProgress.value).toFloat()
                drawRoundRect(
                    color = errorColor.copy(alpha = 0.12f),
                    topLeft = Offset(centerX + gap / 2, size.height - chartHeight),
                    size = Size(barWidth, chartHeight),
                    cornerRadius = CornerRadius(8f, 8f)
                )
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(errorColor, errorColor.copy(alpha = 0.7f)),
                        startY = size.height - expenseHeight,
                        endY = size.height
                    ),
                    topLeft = Offset(centerX + gap / 2, size.height - expenseHeight),
                    size = Size(barWidth, expenseHeight),
                    cornerRadius = CornerRadius(8f, 8f)
                )
            }
        }
        // Labels
        Row(
            modifier = Modifier.fillMaxWidth(),
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
    }
}

/**
 * Biểu đồ Đường (Line Chart) — 2 đường thu nhập/chi tiêu với vùng tô gradient.
 */
@Composable
fun LineChart(
    data: List<BarChartData>,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)

    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(data) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(1f, animationSpec = tween(1000, easing = EaseOutCubic))
    }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            if (data.size < 2) return@Canvas
            val maxValue = data.maxOf { maxOf(it.income, it.expense) }.coerceAtLeast(1.0)
            val stepX = size.width / (data.size - 1)
            val chartHeight = size.height * 0.85f

            // Lưới ngang
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

            fun valueToY(value: Double): Float {
                return (size.height - (value / maxValue * chartHeight * animationProgress.value)).toFloat()
            }

            // Vùng tô gradient thu nhập
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
            // Đường thu nhập
            for (i in 0 until data.size - 1) {
                drawLine(
                    color = primaryColor,
                    start = Offset(stepX * i, valueToY(data[i].income)),
                    end = Offset(stepX * (i + 1), valueToY(data[i + 1].income)),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round
                )
            }
            // Chấm tròn trên đường thu nhập
            data.forEachIndexed { i, point ->
                drawCircle(
                    color = primaryColor,
                    radius = 5f,
                    center = Offset(stepX * i, valueToY(point.income))
                )
                drawCircle(
                    color = Color.White,
                    radius = 2.5f,
                    center = Offset(stepX * i, valueToY(point.income))
                )
            }

            // Vùng tô gradient chi tiêu
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
            // Chấm tròn trên đường chi tiêu
            data.forEachIndexed { i, point ->
                drawCircle(
                    color = errorColor,
                    radius = 5f,
                    center = Offset(stepX * i, valueToY(point.expense))
                )
                drawCircle(
                    color = Color.White,
                    radius = 2.5f,
                    center = Offset(stepX * i, valueToY(point.expense))
                )
            }
        }
        // Labels
        Row(
            modifier = Modifier.fillMaxWidth(),
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
    }
}

/**
 * Biểu đồ Trụ Ngang cho phân bổ theo Danh mục — mỗi danh mục một thanh với tên + số tiền.
 */
@Composable
fun CategoryBarChart(
    data: List<CategoryChartData>,
    modifier: Modifier = Modifier
) {
    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(data) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(1f, animationSpec = tween(800, easing = EaseOutCubic))
    }

    val maxAmount = data.maxOfOrNull { it.amount }?.coerceAtLeast(1.0) ?: 1.0
    val total = data.sumOf { it.amount }.coerceAtLeast(1.0)

    Column(modifier = modifier.padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        data.take(6).forEach { item ->
            val barColor = runCatching { Color(android.graphics.Color.parseColor(item.colorHex)) }
                .getOrDefault(MaterialTheme.colorScheme.primary)
            val fraction = (item.amount / maxAmount * animationProgress.value).toFloat()
            val percent = (item.amount / total * 100).toInt()

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${formatCurrency(item.amount)} (${percent}%)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                ) {
                    // Nền
                    drawRoundRect(
                        color = barColor.copy(alpha = 0.15f),
                        size = Size(size.width, size.height),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                    // Thanh giá trị
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(barColor, barColor.copy(alpha = 0.7f))
                        ),
                        size = Size(size.width * fraction, size.height),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                }
            }
        }
    }
}

/**
 * Biểu đồ Đường cho phân bổ theo Danh mục — hiển thị xu hướng/tỉ lệ phân bổ dưới dạng biểu đồ đường.
 */
@Composable
fun CategoryLineChart(
    data: List<CategoryChartData>,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)

    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(data) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(1f, animationSpec = tween(1000, easing = EaseOutCubic))
    }

    val displayData = data.take(6)

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            if (displayData.isEmpty()) return@Canvas
            val maxValue = displayData.maxOf { it.amount }.coerceAtLeast(1.0)
            val chartHeight = size.height * 0.85f

            // Lưới ngang
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

            fun valueToY(value: Double): Float {
                return (size.height - (value / maxValue * chartHeight * animationProgress.value)).toFloat()
            }

            if (displayData.size == 1) {
                val x = size.width / 2
                val y = valueToY(displayData[0].amount)
                val pointColor = runCatching { Color(android.graphics.Color.parseColor(displayData[0].colorHex)) }
                    .getOrDefault(primaryColor)
                drawCircle(
                    color = pointColor,
                    radius = 6f,
                    center = Offset(x, y)
                )
                drawCircle(
                    color = Color.White,
                    radius = 3f,
                    center = Offset(x, y)
                )
            } else {
                val stepX = size.width / (displayData.size - 1)
                
                // Vẽ vùng tô gradient dưới đường
                val fillPath = Path().apply {
                    moveTo(0f, size.height)
                    displayData.forEachIndexed { i, point ->
                        lineTo(stepX * i, valueToY(point.amount))
                    }
                    lineTo(size.width, size.height)
                    close()
                }
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(primaryColor.copy(alpha = 0.25f), primaryColor.copy(alpha = 0.02f))
                    )
                )

                // Vẽ đường nối các điểm
                for (i in 0 until displayData.size - 1) {
                    drawLine(
                        color = primaryColor,
                        start = Offset(stepX * i, valueToY(displayData[i].amount)),
                        end = Offset(stepX * (i + 1), valueToY(displayData[i + 1].amount)),
                        strokeWidth = 3f,
                        cap = StrokeCap.Round
                    )
                }

                // Vẽ chấm tròn với màu của từng category
                displayData.forEachIndexed { i, point ->
                    val pointColor = runCatching { Color(android.graphics.Color.parseColor(point.colorHex)) }
                        .getOrDefault(primaryColor)
                    drawCircle(
                        color = pointColor,
                        radius = 6f,
                        center = Offset(stepX * i, valueToY(point.amount))
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 3f,
                        center = Offset(stepX * i, valueToY(point.amount))
                    )
                }
            }
        }
        
        // Labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = if (displayData.size == 1) Arrangement.Center else Arrangement.SpaceBetween
        ) {
            displayData.forEach { point ->
                Text(
                    text = point.name,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = onSurfaceVariant
                )
            }
        }
    }
}

/** Định dạng số tiền theo kiểu Việt Nam */
fun formatCurrency(amount: Double): String {
    val absAmount = kotlin.math.abs(amount)
    return when {
        absAmount >= 1_000_000_000 -> String.format("%.1fB đ", absAmount / 1_000_000_000)
        absAmount >= 1_000_000 -> String.format("%.1fM đ", absAmount / 1_000_000)
        absAmount >= 1_000 -> String.format("%,.0fK đ", absAmount / 1_000)
        else -> String.format("%,.0f đ", absAmount)
    }
}

/** Định dạng số tiền hiển thị đầy đủ với dấu +/- */
fun formatAmount(amount: Double): String {
    val formatted = String.format("%,.0f", kotlin.math.abs(amount))
    val prefix = if (amount >= 0) "+" else "-"
    return "${prefix}${formatted}đ"
}
