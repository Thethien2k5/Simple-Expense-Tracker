package com.T2V.simple_expense_tracker.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.T2V.simple_expense_tracker.domain.model.Category
import com.T2V.simple_expense_tracker.domain.model.Transaction
import com.T2V.simple_expense_tracker.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Màn hình chính (Dashboard) — hiển thị toàn bộ 5 sections.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onMenuClick: () -> Unit,
    onNotificationClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    var transactionToDetail by remember { mutableStateOf<Transaction?>(null) }
    var transactionToCategorize by remember { mutableStateOf<Transaction?>(null) }

    if (state.isLoading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 100.dp) // Padding for scrolling past FAB if needed
    ) {
        // === Top Bar (Header) ===
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Default.Menu, "Menu", tint = MaterialTheme.colorScheme.onSurface)
            }
            Text(
                text = "Simple Expense Tracker",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            IconButton(onClick = onNotificationClick) {
                BadgedBox(
                    badge = {
                        val unprocessedCount = state.pendingTransactions.size
                        if (unprocessedCount > 0) {
                            Badge(containerColor = MaterialTheme.colorScheme.error) {
                                Text(unprocessedCount.toString(), color = MaterialTheme.colorScheme.onError)
                            }
                        }
                    }
                ) {
                    Icon(Icons.Default.Notifications, "Notifications", tint = MaterialTheme.colorScheme.onSurface)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // === Section 1: Tổng số dư ===
        BalanceSection(
            state = state
        )

        Spacer(modifier = Modifier.height(32.dp))

        // === Section 2: Chờ xử lý ===
        if (state.pendingTransactions.isNotEmpty()) {
            PendingSection(
                pendingTransactions = state.pendingTransactions,
                onCategorizeClick = { transactionToCategorize = it },
                onItemClick = { transactionToDetail = it }
            )
            Spacer(modifier = Modifier.height(32.dp))
        }

        // === Section 3: Giao dịch gần đây ===
        CollapsibleSection(
            title = "Giao dịch gần đây",
            modifier = Modifier.padding(horizontal = 20.dp),
            initiallyExpanded = true
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                state.categorizedTransactions.take(5).forEach { transaction ->
                    val category = state.getCategoryById(transaction.categoryId)
                    RecentTransactionItem(
                        transaction = transaction,
                        category = category,
                        onClick = { transactionToDetail = it }
                    )
                }
                if (state.categorizedTransactions.isEmpty()) {
                    Text(
                        "Chưa có giao dịch nào được phân loại.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // === Section 4: Danh sách chi tiết ===
        DetailListSection(
            state = state,
            onDateSelected = { viewModel.selectDate(it) },
            onTransactionClick = { transactionToDetail = it }
        )

        Spacer(modifier = Modifier.height(32.dp))

        // === Section 5: Thống kê chi tiêu ===
        StatsSection(
            state = state,
            viewModel = viewModel
        )
    }

    // === Dialogs ===
    transactionToDetail?.let { tx ->
        TransactionDetailDialog(
            transaction = tx,
            bankName = state.getBankNameById(tx.bankAccountId),
            category = state.getCategoryById(tx.categoryId),
            onDismiss = { transactionToDetail = null }
        )
    }

    transactionToCategorize?.let { tx ->
        CategorySelectDialog(
            categories = state.categories,
            onCategorySelected = { category ->
                viewModel.categorizeTransaction(tx, category)
                transactionToCategorize = null
            },
            onDismiss = { transactionToCategorize = null }
        )
    }
}

@Composable
private fun BalanceSection(
    state: DashboardUiState
) {
    var showAccounts by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Tổng số dư",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = formatAmount(state.totalBalance).replace("+", "").replace("-", ""), // Display absolute total balance
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .clickable { showAccounts = !showAccounts }
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Xem tài khoản",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.rotate(if (showAccounts) 180f else 0f)
            )
        }
 
        AnimatedVisibility(visible = showAccounts) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                state.bankAccounts.forEach { account ->
                    val color = runCatching { Color(android.graphics.Color.parseColor(account.colorHex)) }
                        .getOrDefault(MaterialTheme.colorScheme.primary)
                    val balance = state.getAccountBalance(account.id)
                    val formattedBalance = formatAmount(balance).replace("+", "")
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceContainerLow)
                            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically, 
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                            Column {
                                Text(
                                    text = account.bankName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = account.accountNumber,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            text = formattedBalance,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (balance < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PendingSection(
    pendingTransactions: List<Transaction>,
    onCategorizeClick: (Transaction) -> Unit,
    onItemClick: (Transaction) -> Unit
) {
    Column {
        Text(
            text = "Chờ xử lý (${pendingTransactions.size})",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 16.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(pendingTransactions) { tx ->
                Card(
                    modifier = Modifier
                        .width(260.dp)
                        .clickable { onItemClick(tx) },
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SurfaceContainerLowest),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column {
                                Text(
                                    text = tx.counterparty.takeIf { it.isNotBlank() } ?: "Không xác định",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                                Text(
                                    text = timeFormat.format(Date(tx.timestamp)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        val isExpense = tx.amount < 0
                        Text(
                            text = formatAmount(tx.amount),
                            style = MaterialTheme.typography.headlineMedium,
                            color = if (isExpense) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { onCategorizeClick(tx) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Chọn danh mục", color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentTransactionItem(
    transaction: Transaction,
    category: Category?,
    onClick: (Transaction) -> Unit
) {
    val isExpense = transaction.amount < 0
    val catColor = category?.let { 
        runCatching { Color(android.graphics.Color.parseColor(it.colorHex)) }.getOrNull()
    } ?: MaterialTheme.colorScheme.surfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(transaction) }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(catColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    tint = catColor
                )
            }
            Column {
                Text(
                    text = transaction.counterparty.takeIf { it.isNotBlank() } ?: "Không xác định",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = category?.name ?: "Chưa phân loại",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            text = formatAmount(transaction.amount),
            style = MaterialTheme.typography.bodyLarge,
            color = if (isExpense) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailListSection(
    state: DashboardUiState,
    onDateSelected: (Long) -> Unit,
    onTransactionClick: (Transaction) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.selectedDate
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        onDateSelected(it)
                    }
                    showDatePicker = false
                }) {
                    Text("OK", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Hủy", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    headlineContentColor = MaterialTheme.colorScheme.onSurface,
                    selectedDayContainerColor = MaterialTheme.colorScheme.primary,
                    selectedDayContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }

    // Tính toán 7 ngày trong tuần chứa selectedDate, luôn bắt đầu từ Chủ Nhật (CN) đến Thứ Bảy (T7)
    val dates = remember(state.selectedDate) {
        val cal = Calendar.getInstance().apply {
            timeInMillis = state.selectedDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            
            val dayOfWeek = get(Calendar.DAY_OF_WEEK)
            // Lùi số ngày tương ứng để về Chủ Nhật đầu tuần (Calendar.SUNDAY = 1)
            add(Calendar.DAY_OF_YEAR, -(dayOfWeek - Calendar.SUNDAY))
        }
        (0..6).map {
            val dayCal = cal.clone() as Calendar
            dayCal.add(Calendar.DAY_OF_YEAR, it)
            dayCal.timeInMillis
        }
    }

    CollapsibleSection(
        title = "Danh sách chi tiết",
        modifier = Modifier.padding(horizontal = 20.dp),
        headerExtra = {
            IconButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Chọn ngày",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    ) {
        Column {
            // Phần chọn Ngày / Tháng cũ đã được loại bỏ.
            
            // Scroller hiển thị tuần chứa ngày được chọn
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                dates.forEach { dateInMillis ->
                    val isSelected = isSameDay(dateInMillis, state.selectedDate)
                    val cal = Calendar.getInstance().apply { timeInMillis = dateInMillis }
                    val dayOfWeek = when (cal.get(Calendar.DAY_OF_WEEK)) {
                        Calendar.SUNDAY -> "CN"
                        Calendar.MONDAY -> "T2"
                        Calendar.TUESDAY -> "T3"
                        Calendar.WEDNESDAY -> "T4"
                        Calendar.THURSDAY -> "T5"
                        Calendar.FRIDAY -> "T6"
                        Calendar.SATURDAY -> "T7"
                        else -> "CN"
                    }
                    val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH).toString()

                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onDateSelected(dateInMillis) }
                            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else OutlineVariant,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = dayOfWeek,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = dayOfMonth,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Danh sách giao dịch trong ngày được chọn
            if (state.dailyTransactions.isEmpty()) {
                Text(
                    "Không có giao dịch nào trong ngày này.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                state.dailyTransactions.forEach { transaction ->
                    val category = state.getCategoryById(transaction.categoryId)
                    RecentTransactionItem(
                        transaction = transaction,
                        category = category,
                        onClick = onTransactionClick
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatsSection(
    state: DashboardUiState,
    viewModel: DashboardViewModel
) {
    var showFilterDialog by remember { mutableStateOf(false) }
    var showDatePickerForDay by remember { mutableStateOf(false) }
    var showDatePickerForWeek by remember { mutableStateOf(false) }
    var showMonthYearPicker by remember { mutableStateOf(false) }
    var showYearPicker by remember { mutableStateOf(false) }

    CollapsibleSection(
        title = "Thống kê chi tiêu",
        icon = Icons.Default.Info,
        modifier = Modifier.padding(horizontal = 20.dp),
        headerExtra = {
            // Nút bộ lọc: chỉ hiển thị icon Settings theo yêu cầu
            IconButton(
                onClick = { showFilterDialog = true },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Tùy chỉnh thống kê",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    ) {
        Column {
            // === Tabs chọn biểu đồ (Trụ / Đường) — thiết kế chip hiện đại ===
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ChartType.entries.forEach { type ->
                    val isSelected = state.chartType == type
                    val label = when (type) {
                        ChartType.BAR -> "Biểu đồ trụ"
                        ChartType.LINE -> "Biểu đồ đường"
                    }
                    val icon = when (type) {
                        ChartType.BAR -> Icons.Default.List
                        ChartType.LINE -> Icons.Default.PlayArrow
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.selectChartType(type) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Đã loại bỏ 2 nút "Thu / Chi" và "Phân loại" khỏi màn hình chính theo yêu cầu

            // === KPI Summary Cards ===
            when (state.statsViewMode) {
                StatsViewMode.INCOME_EXPENSE -> {
                    // Thẻ tóm tắt thu/chi/số dư
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Thẻ Thu nhập
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                            Icon(
                                                Icons.Default.Add,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Thu nhập", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = formatCurrency(state.statsIncome),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        // Thẻ Chi tiêu
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.08f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                            Icon(
                                                Icons.Default.Clear,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Chi tiêu", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = formatCurrency(state.statsExpense),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                    // Dòng số dư ròng
                    val netColor = if (state.statsNetBalance >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    val netLabel = if (state.statsNetBalance >= 0) "Dư" else "Thâm hụt"
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = netColor.copy(alpha = 0.06f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("$netLabel ròng:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = formatAmount(state.statsNetBalance),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = netColor
                            )
                        }
                    }
                }
                StatsViewMode.CATEGORY -> {
                    val categoryData = state.getCategoryChartData()
                    val totalCat = categoryData.sumOf { it.amount }
                    Text(
                        text = "Tổng: ${formatCurrency(totalCat)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
            }

            // === Chart Box ===
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceContainerLow)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                when (state.statsViewMode) {
                    StatsViewMode.INCOME_EXPENSE -> {
                        val chartData = state.getIncomeExpenseChartData()
                        if (chartData.any { it.income > 0 || it.expense > 0 }) {
                            when (state.chartType) {
                                ChartType.BAR -> BarChart(data = chartData)
                                ChartType.LINE -> LineChart(data = chartData)
                            }
                        } else {
                            Text("Chưa có dữ liệu thống kê", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    StatsViewMode.CATEGORY -> {
                        val categoryData = state.getCategoryChartData()
                        if (categoryData.isNotEmpty()) {
                            when (state.chartType) {
                                ChartType.BAR -> CategoryBarChart(data = categoryData)
                                ChartType.LINE -> CategoryLineChart(data = categoryData)
                            }
                        } else {
                            Text("Chưa có dữ liệu phân loại", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // === Chú giải ===
            if (state.statsViewMode == StatsViewMode.INCOME_EXPENSE) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Thu nhập", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(16.dp))
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Chi tiêu", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }

    // === Filter Dialog (Đổi nền & style giống Chi tiết giao dịch) ===
    if (showFilterDialog) {
        Dialog(
            onDismissRequest = { showFilterDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
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
                    // Header: tiêu đề + nút đóng
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tùy chỉnh thống kê",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(onClick = { showFilterDialog = false }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Đóng",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // 1. Chọn khoảng thời gian
                    Text(
                        text = "KHOẢNG THỜI GIAN",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatsTimePeriod.entries.forEach { period ->
                            val isSelected = state.statsTimePeriod == period
                            val label = when (period) {
                                StatsTimePeriod.DAY -> "Ngày"
                                StatsTimePeriod.WEEK -> "Tuần"
                                StatsTimePeriod.MONTH -> "Tháng"
                                StatsTimePeriod.YEAR -> "Năm"
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.selectTimePeriod(period) }
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 10.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                    Spacer(modifier = Modifier.height(16.dp))

                    // 2. Chọn loại hình
                    Text(
                        text = "LOẠI HÌNH THỐNG KÊ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatsViewMode.entries.forEach { mode ->
                            val isSelected = state.statsViewMode == mode
                            val label = when (mode) {
                                StatsViewMode.INCOME_EXPENSE -> "Thu / Chi"
                                StatsViewMode.CATEGORY -> "Phân loại"
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.selectViewMode(mode) }
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 10.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                    Spacer(modifier = Modifier.height(16.dp))

                    // 3. Chọn cụ thể các thời điểm cần thống kê
                    val listTitle = when (state.statsTimePeriod) {
                        StatsTimePeriod.DAY -> "CÁC NGÀY CẦN THỐNG KÊ"
                        StatsTimePeriod.WEEK -> "CÁC TUẦN CẦN THỐNG KÊ"
                        StatsTimePeriod.MONTH -> "CÁC THÁNG CẦN THỐNG KÊ"
                        StatsTimePeriod.YEAR -> "CÁC NĂM CẦN THỐNG KÊ"
                    }
                    Text(
                        text = listTitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        when (state.statsTimePeriod) {
                            StatsTimePeriod.DAY -> {
                                state.selectedDays.forEach { dayStart ->
                                    val cal = Calendar.getInstance().apply { timeInMillis = dayStart }
                                    val label = String.format("%02d/%02d", cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH) + 1)
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                imageVector = Icons.Default.Clear,
                                                contentDescription = "Xóa",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .clickable { viewModel.removeSelectedDay(dayStart) }
                                            )
                                        }
                                    }
                                }
                            }
                            StatsTimePeriod.WEEK -> {
                                state.selectedWeeks.forEach { weekStart ->
                                    val cal = Calendar.getInstance().apply { timeInMillis = weekStart }
                                    val label = String.format("T.%d (%d/%d)", cal.get(Calendar.WEEK_OF_YEAR), cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH) + 1)
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                imageVector = Icons.Default.Clear,
                                                contentDescription = "Xóa",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .clickable { viewModel.removeSelectedWeek(weekStart) }
                                            )
                                        }
                                    }
                                }
                            }
                            StatsTimePeriod.MONTH -> {
                                state.selectedMonths.forEach { (month, year) ->
                                    val label = String.format("T%d/%d", month + 1, year % 100)
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                imageVector = Icons.Default.Clear,
                                                contentDescription = "Xóa",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .clickable { viewModel.removeSelectedMonth(month, year) }
                                            )
                                        }
                                    }
                                }
                            }
                            StatsTimePeriod.YEAR -> {
                                state.selectedYears.forEach { year ->
                                    val label = year.toString()
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                imageVector = Icons.Default.Clear,
                                                contentDescription = "Xóa",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .clickable { viewModel.removeSelectedYear(year) }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Nút thêm (+)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                            modifier = Modifier.clickable {
                                when (state.statsTimePeriod) {
                                    StatsTimePeriod.DAY -> showDatePickerForDay = true
                                    StatsTimePeriod.WEEK -> showDatePickerForWeek = true
                                    StatsTimePeriod.MONTH -> showMonthYearPicker = true
                                    StatsTimePeriod.YEAR -> showYearPicker = true
                                }
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Thêm", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Nút Đóng
                    Button(
                        onClick = { showFilterDialog = false },
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

    // --- Sub-Dialogs chọn mốc thời gian ---
    if (showDatePickerForDay) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePickerForDay = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        viewModel.addSelectedDay(it)
                    }
                    showDatePickerForDay = false
                }) {
                    Text("OK", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerForDay = false }) {
                    Text("Hủy", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    headlineContentColor = MaterialTheme.colorScheme.onSurface,
                    selectedDayContainerColor = MaterialTheme.colorScheme.primary,
                    selectedDayContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }

    if (showDatePickerForWeek) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePickerForWeek = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        viewModel.addSelectedWeek(it)
                    }
                    showDatePickerForWeek = false
                }) {
                    Text("OK", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerForWeek = false }) {
                    Text("Hủy", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    headlineContentColor = MaterialTheme.colorScheme.onSurface,
                    selectedDayContainerColor = MaterialTheme.colorScheme.primary,
                    selectedDayContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }

    if (showMonthYearPicker) {
        var tempYear by remember { mutableStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
        Dialog(onDismissRequest = { showMonthYearPicker = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Chọn Tháng & Năm", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        IconButton(onClick = { tempYear-- }) {
                            Icon(Icons.Default.ArrowBack, "Năm trước", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(tempYear.toString(), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { tempYear++ }) {
                            Icon(Icons.Default.ArrowForward, "Năm sau", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val months = listOf(
                        listOf(0, 1, 2),
                        listOf(3, 4, 5),
                        listOf(6, 7, 8),
                        listOf(9, 10, 11)
                    )
                    months.forEach { rowMonths ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowMonths.forEach { m ->
                                Button(
                                    onClick = {
                                        viewModel.addSelectedMonth(m, tempYear)
                                        showMonthYearPicker = false
                                    },
                                    modifier = Modifier.weight(1f).padding(vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                        contentColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Text("Th. ${m + 1}", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = { showMonthYearPicker = false }) {
                        Text("Hủy", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    if (showYearPicker) {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val years = (currentYear - 5..currentYear + 2).toList()
        Dialog(onDismissRequest = { showYearPicker = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceContainerHigh),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Chọn Năm", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val chunkedYears = years.chunked(3)
                    chunkedYears.forEach { rowYears ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowYears.forEach { y ->
                                Button(
                                    onClick = {
                                        viewModel.addSelectedYear(y)
                                        showYearPicker = false
                                    },
                                    modifier = Modifier.weight(1f).padding(vertical = 4.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                        contentColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Text(y.toString(), style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = { showYearPicker = false }) {
                        Text("Hủy", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

private fun isSameDay(time1: Long, time2: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = time1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = time2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
           cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}
