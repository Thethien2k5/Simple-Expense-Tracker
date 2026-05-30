package com.T2V.simple_expense_tracker.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.T2V.simple_expense_tracker.domain.model.Transaction
import com.T2V.simple_expense_tracker.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Dialog hiển thị chi tiết một giao dịch — khớp với thiết kế "Chi tiết giao dịch".
 * Hiển thị: Nguồn, Thời gian, Số tiền (màu theo thu/chi), Danh mục, Nội dung.
 */
@Composable
fun TransactionDetailDialog(
    transaction: Transaction,
    bankName: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        // Backdrop mờ đã được Dialog tự xử lý
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                // Header: tiêu đề + nút đóng
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Chi tiết giao dịch",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Đóng",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Row: Nguồn
                DetailRow(label = "Nguồn", value = bankName)
                // Row: Thời gian
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val timeStr = dateFormat.format(Date(transaction.timestamp))
                DetailRow(label = "Thời gian", value = timeStr)
                // Row: Số tiền
                val isExpense = transaction.amount < 0
                DetailRow(
                    label = "Số tiền",
                    value = formatAmount(transaction.amount),
                    valueColor = if (isExpense) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    valueFontWeight = FontWeight.SemiBold
                )
                // Removed Danh mục

                // Box: Nội dung chuyển khoản
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "NỘI DUNG CHUYỂN KHOẢN",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(12.dp)
                ) {
                    Text(
                        text = transaction.content.ifEmpty { "Không có nội dung" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontStyle = FontStyle.Italic
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Nút Đóng
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = "Đóng",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/**
 * Hàng chi tiết với nhãn bên trái và giá trị bên phải,
 * phân cách bằng border dưới mờ.
 */
@Composable
private fun DetailRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    valueFontWeight: FontWeight = FontWeight.Medium
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
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
                fontWeight = valueFontWeight
            )
        }
        HorizontalDivider(
            color = DividerMuted
        )
    }
}
