package com.T2V.simple_expense_tracker.ui.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.T2V.simple_expense_tracker.ui.theme.*
import kotlin.math.abs

// ==========================================================================
// AnomalyConfirmationDialog.kt
// Hộp thoại cảnh báo khi phát hiện chênh lệch (bất thường) số dư ngân hàng.
// Hiển thị thông tin so sánh giữa số dư dự kiến và số dư thông báo,
// cho phép người dùng xác nhận cập nhật hoặc bỏ qua.
// ==========================================================================

/**
 * Hộp thoại xác nhận khi phát hiện sai lệch số dư ngân hàng.
 *
 * @param bankName       Tên ngân hàng phát hiện bất thường
 * @param currentBalance Số dư hiện tại lưu trong cơ sở dữ liệu (VND)
 * @param transactionAmount Số tiền giao dịch vừa xảy ra (VND)
 * @param expectedBalance Số dư dự kiến sau giao dịch = currentBalance ± transactionAmount (VND)
 * @param reportedBalance Số dư thực tế từ thông báo ngân hàng (VND)
 * @param onConfirm      Callback khi người dùng đồng ý cập nhật số dư
 * @param onDismiss      Callback khi người dùng bỏ qua cảnh báo
 */
@Composable
fun AnomalyConfirmationDialog(
    bankName: String,
    currentBalance: Double,
    transactionAmount: Double,
    expectedBalance: Double,
    reportedBalance: Double,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    // Tính chênh lệch giữa số dư thông báo và số dư dự kiến
    val difference = reportedBalance - expectedBalance

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = SurfaceContainerHigh
            )
        ) {
            Column(modifier = Modifier.padding(24.dp)) {

                // ── Banner cảnh báo phía trên ──
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Cảnh báo",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Phát hiện bất thường số dư",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ── Bảng thông tin chi tiết ──
                InfoRow(label = "Ngân hàng", value = bankName)
                InfoRow(
                    label = "Số dư hiện tại (DB)",
                    value = formatVnd(currentBalance)
                )
                InfoRow(
                    label = "Số tiền giao dịch",
                    value = formatVnd(transactionAmount),
                    valueColor = if (transactionAmount >= 0)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.error
                )
                InfoRow(
                    label = "Số dư dự kiến",
                    value = formatVnd(expectedBalance)
                )
                InfoRow(
                    label = "Số dư thông báo",
                    value = formatVnd(reportedBalance)
                )

                Spacer(modifier = Modifier.height(4.dp))

                // ── Hàng chênh lệch — nổi bật ──
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f))
                        .padding(horizontal = 12.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Chênh lệch",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = formatVnd(difference),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // ── Nút hành động ──
                // Nút chính: Đồng ý cập nhật
                Button(
                    onClick = onConfirm,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = "Đồng ý cập nhật",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

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
            }
        }
    }
}

// ─────────────────────────────────────────────
// Composable nội bộ: Hàng thông tin (label — value)
// ─────────────────────────────────────────────

/**
 * Hàng chi tiết hiển thị nhãn bên trái và giá trị bên phải,
 * phân cách bằng border dưới mờ, nhất quán với TransactionDetailDialog.
 */
@Composable
private fun InfoRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = valueColor,
                fontWeight = FontWeight.Medium
            )
        }
        HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
    }
}

// ─────────────────────────────────────────────
// Hàm tiện ích: Định dạng số tiền VND
// ─────────────────────────────────────────────

/**
 * Định dạng số tiền theo chuẩn VND với dấu phân cách hàng nghìn.
 * Số dương sẽ có dấu "+", số âm sẽ giữ nguyên dấu "-".
 *
 * Ví dụ: 1500000.0 → "+1,500,000 VND"
 *        -200000.0 → "-200,000 VND"
 */
private fun formatVnd(amount: Double): String {
    val formatter = java.text.DecimalFormat("#,###")
    return "${if (amount >= 0) "+" else ""}${formatter.format(amount)} VND"
}

// ─────────────────────────────────────────────
// Preview
// ─────────────────────────────────────────────

@Preview(showBackground = true, backgroundColor = 0xFF0B1326)
@Composable
private fun AnomalyConfirmationDialogPreview() {
    SimpleExpenseTrackerTheme {
        AnomalyConfirmationDialog(
            bankName = "Vietcombank",
            currentBalance = 5_000_000.0,
            transactionAmount = -500_000.0,
            expectedBalance = 4_500_000.0,
            reportedBalance = 4_200_000.0,
            onConfirm = {},
            onDismiss = {}
        )
    }
}
