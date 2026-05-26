package com.T2V.simple_expense_tracker.ui.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Section thu gọn/mở rộng tái sử dụng — tương ứng với pattern
 * CSS peer-checked:grid-rows-[1fr] trong thiết kế HTML.
 */
@Composable
fun CollapsibleSection(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    initiallyExpanded: Boolean = true,
    headerExtra: @Composable (RowScope.() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(300),
        label = "expandRotation"
    )

    Column(modifier = modifier) {
        // Header row: tiêu đề + nội dung phụ + icon mở rộng
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
//                if (icon != null) {
//                    Icon(
//                        imageVector = icon,
//                        contentDescription = null,
//                        tint = MaterialTheme.colorScheme.primary,
//                        modifier = Modifier.size(22.dp)
//                    )
//                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(8.dp))
                if (headerExtra != null) {
                    headerExtra()
                }
            }
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = if (expanded) "Thu gọn" else "Mở rộng",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.rotate(rotationAngle)
            )
        }

        // Nội dung với animation mở rộng/thu gọn
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)),
            exit = shrinkVertically(animationSpec = tween(300)) + fadeOut(animationSpec = tween(300))
        ) {
            content()
        }
    }
}
