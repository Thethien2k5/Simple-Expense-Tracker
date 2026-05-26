package com.T2V.simple_expense_tracker.ui.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.T2V.simple_expense_tracker.domain.model.Category
import com.T2V.simple_expense_tracker.ui.theme.*

/**
 * Panel "Tùy chỉnh" — hiển thị trong drawer bên trái.
 * Gồm 2 section: "Loại hình giao dịch" (Categories) và "Cài đặt".
 */
@Composable
fun SettingsPanel(
    viewModel: SettingsViewModel,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
    ) {
        // Header bar giống thiết kế: menu | Tùy chỉnh
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Đóng",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = "Tùy chỉnh",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Scrollable content
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // === Section 1: Loại hình giao dịch ===
            item {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Phân loại giao dịch",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "Quản lý các loại giao dịch để phân loại chi tiêu.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
            }

            // Danh sách Categories
            items(state.categories) { category ->
                CategoryCard(category = category)
            }

            // Nút thêm danh mục mới
            item {
                OutlinedButton(
                    onClick = { viewModel.toggleAddCategoryDialog(true) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(OutlineVariant)
                    )
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Thêm loại giao dịch",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // === Section 2: Cài đặt ===
            item {
                Text(
                    text = "Cài đặt",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceContainerLow)
                ) {
                    // Ngôn ngữ
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { /* TODO: Chọn ngôn ngữ */ }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Ngôn ngữ",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Tiếng Việt",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Icon(
                                Icons.Default.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

                    // Giao diện
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { /* TODO: Đổi giao diện */ }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Giao diện",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Tối (Dark)",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialog thêm danh mục mới
    if (state.showAddCategoryDialog) {
        AddCategoryDialog(
            name = state.newCategoryName,
            onNameChange = { viewModel.updateNewCategoryName(it) },
            onSave = { viewModel.saveNewCategory() },
            onDismiss = { viewModel.toggleAddCategoryDialog(false) }
        )
    }
}

/**
 * Card hiển thị một loại giao dịch (Category) — theo thiết kế tuychinh.txt.
 * Gồm: tên, icon + tag, toggle, độ tin cậy.
 */
@Composable
private fun CategoryCard(category: Category) {
    val catColor = runCatching {
        Color(android.graphics.Color.parseColor(category.colorHex))
    }.getOrDefault(MaterialTheme.colorScheme.primary)

    var isEnabled by remember { mutableStateOf(true) }
    val cardAlpha = if (isEnabled) 1f else 0.7f

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceContainerLow.copy(alpha = cardAlpha)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Row trên: tên + toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(catColor)
                        )
                        Text(
                            text = category.iconRes,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                // Toggle switch
                Switch(
                    checked = isEnabled,
                    onCheckedChange = { isEnabled = it },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedTrackColor = SurfaceContainer
                    )
                )
            }

            // Độ tin cậy (giả lập)
            HorizontalDivider(
                color = Color.White.copy(alpha = 0.05f),
                modifier = Modifier.padding(vertical = 12.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Độ tin cậy",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // 5 chấm tin cậy — giả lập dựa trên category id
                    val filled = ((category.id % 5) + 1).toInt().coerceIn(1, 5)
                    repeat(5) { index ->
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (index < filled)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        SurfaceVariant
                                )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Dialog thêm loại giao dịch mới — form đơn giản với tên và nút lưu.
 */
@Composable
private fun AddCategoryDialog(
    name: String,
    onNameChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceContainerHigh,
        title = {
            Text(
                "Thêm loại giao dịch",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("Tên loại giao dịch") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = OutlineVariant,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onSave() })
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Lưu", color = MaterialTheme.colorScheme.onPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}
