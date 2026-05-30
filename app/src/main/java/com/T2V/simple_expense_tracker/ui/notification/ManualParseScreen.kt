package com.T2V.simple_expense_tracker.ui.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.T2V.simple_expense_tracker.ui.theme.*

// ==========================================================================
// ManualParseScreen.kt
// Màn hình cho phép người dùng xem nội dung thông báo thô (raw)
// và nhập thủ công thông tin giao dịch khi hệ thống không thể tự phân tích.
// ==========================================================================

/**
 * Màn hình xử lý thông báo thủ công.
 *
 * @param rawContent  Nội dung thô của thông báo ngân hàng gốc
 * @param bankName    Tên ngân hàng (nguồn thông báo)
 * @param onSave      Callback lưu giao dịch thủ công
 * @param onDismiss   Callback bỏ qua / quay lại
 * @param modifier    Modifier tùy chỉnh từ bên ngoài
 */
@Composable
fun ManualParseScreen(
    rawContent: String,
    bankName: String,
    onSave: (amount: Double, isCredit: Boolean, accountNumber: String, content: String, counterparty: String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // ── State cho các trường nhập liệu ──
    var amountText by remember { mutableStateOf("") }
    var isCredit by remember { mutableStateOf(false) }  // false = Chi tiêu, true = Thu nhập
    var accountNumber by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var counterparty by remember { mutableStateOf("") }

    // Kiểm tra dữ liệu hợp lệ: số tiền phải > 0
    val isAmountValid = amountText.toDoubleOrNull()?.let { it > 0 } ?: false

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
    ) {
        // ── Header ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Nút quay lại
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Quay lại",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Column {
                Text(
                    text = "Xử lý thông báo thủ công",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = bankName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // ── Nội dung cuộn được ──
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Card 1: Nội dung thông báo gốc ──
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Nhãn mục
                    Text(
                        text = "NỘI DUNG THÔNG BÁO GỐC",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Box hiển thị nội dung thô với nền tối hơn
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceContainerLowest)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = rawContent.ifEmpty { "Không có nội dung" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontStyle = FontStyle.Italic
                        )
                    }
                }
            }

            // ── Card 2: Form nhập thông tin giao dịch ──
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "THÔNG TIN GIAO DỊCH",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // ── Loại giao dịch: Thu nhập / Chi tiêu ──
                    TransactionTypeSelector(
                        isCredit = isCredit,
                        onTypeChanged = { isCredit = it }
                    )

                    // ── Số tiền ──
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { newValue ->
                            // Chỉ cho phép nhập số và dấu thập phân
                            if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                                amountText = newValue
                            }
                        },
                        label = { Text("Số tiền (VND)") },
                        placeholder = { Text("Ví dụ: 500000") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        isError = amountText.isNotEmpty() && !isAmountValid,
                        supportingText = if (amountText.isNotEmpty() && !isAmountValid) {
                            { Text("Vui lòng nhập số tiền hợp lệ (lớn hơn 0)") }
                        } else null,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = manualParseTextFieldColors()
                    )

                    // ── Số tài khoản ──
                    OutlinedTextField(
                        value = accountNumber,
                        onValueChange = { accountNumber = it },
                        label = { Text("Số tài khoản") },
                        placeholder = { Text("Ví dụ: 0123456789") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.AccountBox,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = manualParseTextFieldColors()
                    )

                    // ── Nội dung giao dịch ──
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("Nội dung giao dịch") },
                        placeholder = { Text("Ví dụ: Thanh toán hóa đơn điện") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = manualParseTextFieldColors()
                    )

                    // ── Đối tác (counterparty) ──
                    OutlinedTextField(
                        value = counterparty,
                        onValueChange = { counterparty = it },
                        label = { Text("Đối tác") },
                        placeholder = { Text("Ví dụ: Công ty ABC") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = manualParseTextFieldColors()
                    )
                }
            }

            // ── Nút hành động ──
            // Nút chính: Lưu giao dịch
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: return@Button
                    onSave(amount, isCredit, accountNumber, content, counterparty)
                },
                enabled = isAmountValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Lưu giao dịch",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Medium
                )
            }

            // Nút phụ: Bỏ qua
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text(
                    text = "Bỏ qua",
                    fontWeight = FontWeight.Medium
                )
            }

            // Khoảng trống cuối cho thanh điều hướng
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

// ─────────────────────────────────────────────
// Composable nội bộ: Bộ chọn loại giao dịch
// ─────────────────────────────────────────────

/**
 * Bộ chọn loại giao dịch dạng 2 nút phân đoạn (segmented):
 * - "Chi tiêu" (mặc định) — màu error
 * - "Thu nhập" — màu primary
 */
@Composable
private fun TransactionTypeSelector(
    isCredit: Boolean,
    onTypeChanged: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Nút Chi tiêu (isCredit = false)
        SegmentButton(
            text = "Chi tiêu",
            icon = Icons.Default.KeyboardArrowDown,
            isSelected = !isCredit,
            selectedColor = MaterialTheme.colorScheme.error,
            onClick = { onTypeChanged(false) },
            modifier = Modifier.weight(1f)
        )
        // Nút Thu nhập (isCredit = true)
        SegmentButton(
            text = "Thu nhập",
            icon = Icons.Default.KeyboardArrowUp,
            isSelected = isCredit,
            selectedColor = MaterialTheme.colorScheme.primary,
            onClick = { onTypeChanged(true) },
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Nút phân đoạn đơn lẻ — đổi màu nền + viền khi được chọn.
 */
@Composable
private fun SegmentButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isSelected)
        selectedColor.copy(alpha = 0.15f)
    else
        SurfaceContainerLowest

    val contentColor = if (isSelected)
        selectedColor
    else
        MaterialTheme.colorScheme.onSurfaceVariant

    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(1.5.dp, selectedColor.copy(alpha = 0.5f))
        } else {
            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

// ─────────────────────────────────────────────
// Hàm tiện ích: Màu cho OutlinedTextField
// ─────────────────────────────────────────────

/**
 * Bộ màu thống nhất cho các OutlinedTextField trong màn hình này.
 * Sử dụng màu từ theme để đảm bảo giao diện tối nhất quán.
 */
@Composable
private fun manualParseTextFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    cursorColor = MaterialTheme.colorScheme.primary,
    focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
    unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
    errorBorderColor = MaterialTheme.colorScheme.error,
    errorLabelColor = MaterialTheme.colorScheme.error,
    errorSupportingTextColor = MaterialTheme.colorScheme.error
)

// ─────────────────────────────────────────────
// Preview
// ─────────────────────────────────────────────

@Preview(showBackground = true, backgroundColor = 0xFF0B1326, showSystemUi = true)
@Composable
private fun ManualParseScreenPreview() {
    SimpleExpenseTrackerTheme(theme = AppTheme.EMERALD) {
        ManualParseScreen(
            rawContent = "VCB: TK 0123456789 so du -500,000VND luc 29/05/2026 15:30. SD: 4,500,000VND. Noi dung: Thanh toan hoa don dien.",
            bankName = "Vietcombank",
            onSave = { _, _, _, _, _ -> },
            onDismiss = {}
        )
    }
}
