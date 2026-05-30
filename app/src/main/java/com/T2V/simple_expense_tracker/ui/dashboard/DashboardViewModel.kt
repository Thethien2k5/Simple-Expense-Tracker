package com.T2V.simple_expense_tracker.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.T2V.simple_expense_tracker.domain.model.BankAccount
import com.T2V.simple_expense_tracker.domain.model.Transaction
import com.T2V.simple_expense_tracker.domain.usecase.GetBankAccountsUseCase
import com.T2V.simple_expense_tracker.domain.usecase.GetTransactionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

/** Khoảng thời gian thống kê */
enum class StatsTimePeriod { DAY, WEEK, MONTH, YEAR }

/** Loại biểu đồ: chỉ Trụ và Đường */
enum class ChartType { BAR, LINE }

/**
 * Trạng thái UI của Dashboard — chứa tất cả dữ liệu cần thiết để render 5 section.
 */
data class DashboardUiState(
    val isLoading: Boolean = true,
    val bankAccounts: List<BankAccount> = emptyList(),
    val allTransactions: List<Transaction> = emptyList(),
    val selectedDate: Long = System.currentTimeMillis(),
    val chartType: ChartType = ChartType.BAR,
    val statsTimePeriod: StatsTimePeriod = StatsTimePeriod.DAY,
    val selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH),
    val selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val selectedDays: List<Long> = getDefaultDays(),
    val selectedWeeks: List<Long> = getDefaultWeeks(),
    val selectedMonths: List<Pair<Int, Int>> = getDefaultMonths(),
    val selectedYears: List<Int> = getDefaultYears()
) {
    companion object {
        fun getDefaultDays(): List<Long> {
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            val d0 = cal.timeInMillis
            cal.add(Calendar.DAY_OF_YEAR, -1)
            val d1 = cal.timeInMillis
            cal.add(Calendar.DAY_OF_YEAR, -1)
            val d2 = cal.timeInMillis
            return listOf(d2, d1, d0)
        }

        fun getDefaultWeeks(): List<Long> {
            val cal = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            val w0 = cal.timeInMillis
            cal.add(Calendar.WEEK_OF_YEAR, -1)
            val w1 = cal.timeInMillis
            cal.add(Calendar.WEEK_OF_YEAR, -1)
            val w2 = cal.timeInMillis
            return listOf(w2, w1, w0)
        }

        fun getDefaultMonths(): List<Pair<Int, Int>> {
            val cal = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            val m0 = Pair(cal.get(Calendar.MONTH), cal.get(Calendar.YEAR))
            cal.add(Calendar.MONTH, -1)
            val m1 = Pair(cal.get(Calendar.MONTH), cal.get(Calendar.YEAR))
            cal.add(Calendar.MONTH, -1)
            val m2 = Pair(cal.get(Calendar.MONTH), cal.get(Calendar.YEAR))
            return listOf(m2, m1, m0)
        }

        fun getDefaultYears(): List<Int> {
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            return listOf(currentYear - 2, currentYear - 1, currentYear)
        }
    }

    /** Tổng số dư = tổng số dư của tất cả tài khoản ngân hàng */
    val totalBalance: Double get() = bankAccounts.sumOf { it.balance }

    /** Giao dịch diễn ra trong ngày hôm nay (thay cho Giao dịch đã phân loại) */
    val recentTransactions: List<Transaction>
        get() {
            val todayStart = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val todayEnd = todayStart + 86_400_000L
            return allTransactions.filter { it.timestamp in todayStart until todayEnd }
        }

    /** Giao dịch theo ngày được chọn */
    val dailyTransactions: List<Transaction>
        get() {
            val cal = Calendar.getInstance().apply { timeInMillis = selectedDate }
            val dayStart = cal.apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val dayEnd = dayStart + 86_400_000L
            return allTransactions.filter { it.timestamp in dayStart until dayEnd }
        }

    /** Tổng thu trong khoảng thời gian thống kê hiện tại */
    val statsIncome: Double
        get() = getStatsTransactions().filter { it.amount > 0 }.sumOf { it.amount }

    /** Tổng chi trong khoảng thời gian thống kê hiện tại */
    val statsExpense: Double
        get() = getStatsTransactions().filter { it.amount < 0 }.sumOf { kotlin.math.abs(it.amount) }

    /** Số dư ròng (Thu - Chi) trong khoảng thời gian thống kê */
    val statsNetBalance: Double
        get() = statsIncome - statsExpense

    /**
     * Lấy danh sách giao dịch nằm trong khoảng thời gian thống kê hiện tại.
     */
    fun getStatsTransactions(): List<Transaction> {
        return when (statsTimePeriod) {
            StatsTimePeriod.DAY -> {
                allTransactions.filter { tx ->
                    selectedDays.any { startOfDay ->
                        tx.timestamp in startOfDay until (startOfDay + 86_400_000L)
                    }
                }
            }
            StatsTimePeriod.WEEK -> {
                allTransactions.filter { tx ->
                    selectedWeeks.any { startOfWeek ->
                        tx.timestamp in startOfWeek until (startOfWeek + 7 * 86_400_000L)
                    }
                }
            }
            StatsTimePeriod.MONTH -> {
                allTransactions.filter { tx ->
                    selectedMonths.any { (month, year) ->
                        val cal = Calendar.getInstance().apply {
                            set(Calendar.YEAR, year)
                            set(Calendar.MONTH, month)
                            set(Calendar.DAY_OF_MONTH, 1)
                            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                        }
                        val start = cal.timeInMillis
                        cal.add(Calendar.MONTH, 1)
                        val end = cal.timeInMillis
                        tx.timestamp in start until end
                    }
                }
            }
            StatsTimePeriod.YEAR -> {
                allTransactions.filter { tx ->
                    selectedYears.any { year ->
                        val cal = Calendar.getInstance().apply {
                            set(Calendar.YEAR, year)
                            set(Calendar.MONTH, 0)
                            set(Calendar.DAY_OF_MONTH, 1)
                            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                        }
                        val start = cal.timeInMillis
                        cal.add(Calendar.YEAR, 1)
                        val end = cal.timeInMillis
                        tx.timestamp in start until end
                    }
                }
            }
        }
    }

    /**
     * Dữ liệu biểu đồ Trụ/Đường cho chế độ Thu/Chi — so sánh song song các thời điểm được chọn.
     */
    fun getIncomeExpenseChartData(): List<BarChartData> {
        return when (statsTimePeriod) {
            StatsTimePeriod.DAY -> {
                selectedDays.sorted().map { startOfDay ->
                    val cal = Calendar.getInstance().apply { timeInMillis = startOfDay }
                    val label = String.format("%02d/%02d", cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH) + 1)
                    val dayTransactions = allTransactions.filter { it.timestamp in startOfDay until (startOfDay + 86_400_000L) }
                    val income = dayTransactions.filter { it.amount > 0 }.sumOf { it.amount }
                    val expense = dayTransactions.filter { it.amount < 0 }.sumOf { kotlin.math.abs(it.amount) }
                    BarChartData(label, income, expense)
                }
            }
            StatsTimePeriod.WEEK -> {
                selectedWeeks.sorted().map { startOfWeek ->
                    val cal = Calendar.getInstance().apply { timeInMillis = startOfWeek }
                    val weekOfYear = cal.get(Calendar.WEEK_OF_YEAR)
                    val label = String.format("T.%d (%d/%d)", weekOfYear, cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH) + 1)
                    val weekTransactions = allTransactions.filter { it.timestamp in startOfWeek until (startOfWeek + 7 * 86_400_000L) }
                    val income = weekTransactions.filter { it.amount > 0 }.sumOf { it.amount }
                    val expense = weekTransactions.filter { it.amount < 0 }.sumOf { kotlin.math.abs(it.amount) }
                    BarChartData(label, income, expense)
                }
            }
            StatsTimePeriod.MONTH -> {
                selectedMonths.sortedWith(compareBy<Pair<Int, Int>> { it.second }.thenBy { it.first }).map { (month, year) ->
                    val cal = Calendar.getInstance().apply {
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, month)
                        set(Calendar.DAY_OF_MONTH, 1)
                        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                    }
                    val start = cal.timeInMillis
                    cal.add(Calendar.MONTH, 1)
                    val end = cal.timeInMillis
                    val label = String.format("T%d/%d", month + 1, year % 100)
                    val monthTransactions = allTransactions.filter { it.timestamp in start until end }
                    val income = monthTransactions.filter { it.amount > 0 }.sumOf { it.amount }
                    val expense = monthTransactions.filter { it.amount < 0 }.sumOf { kotlin.math.abs(it.amount) }
                    BarChartData(label, income, expense)
                }
            }
            StatsTimePeriod.YEAR -> {
                selectedYears.sorted().map { year ->
                    val cal = Calendar.getInstance().apply {
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, 0)
                        set(Calendar.DAY_OF_MONTH, 1)
                        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                    }
                    val start = cal.timeInMillis
                    cal.add(Calendar.YEAR, 1)
                    val end = cal.timeInMillis
                    val label = year.toString()
                    val yearTransactions = allTransactions.filter { it.timestamp in start until end }
                    val income = yearTransactions.filter { it.amount > 0 }.sumOf { it.amount }
                    val expense = yearTransactions.filter { it.amount < 0 }.sumOf { kotlin.math.abs(it.amount) }
                    BarChartData(label, income, expense)
                }
            }
        }
    }

    /** Tìm BankAccount theo ID */
    fun getBankNameById(id: Long): String =
        bankAccounts.find { it.id == id }?.bankName ?: "Không rõ"

    /** Tính số dư của một tài khoản ngân hàng cụ thể từ trường balance */
    fun getAccountBalance(bankAccountId: Long): Double {
        return bankAccounts.find { it.id == bankAccountId }?.balance ?: 0.0
    }
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    getBankAccountsUseCase: GetBankAccountsUseCase,
    getTransactionsUseCase: GetTransactionsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        // Kết hợp 2 luồng dữ liệu từ DB — khi bất kỳ nguồn nào thay đổi, UI tự cập nhật
        viewModelScope.launch {
            combine(
                getBankAccountsUseCase(),
                getTransactionsUseCase()
            ) { accounts, transactions ->
                _uiState.value.copy(
                    isLoading = false,
                    bankAccounts = accounts,
                    allTransactions = transactions
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    /** Thay đổi ngày được chọn trong bộ lọc "Danh sách chi tiết" */
    fun selectDate(timestamp: Long) {
        _uiState.update { it.copy(selectedDate = timestamp) }
    }

    /** Thay đổi loại biểu đồ */
    fun selectChartType(type: ChartType) {
        _uiState.update { it.copy(chartType = type) }
    }

    /** Thay đổi khoảng thời gian thống kê */
    fun selectTimePeriod(period: StatsTimePeriod) {
        _uiState.update { it.copy(statsTimePeriod = period) }
    }

    /** Thay đổi tháng thống kê */
    fun selectMonth(month: Int) {
        _uiState.update { it.copy(selectedMonth = month) }
    }

    /** Thay đổi năm thống kê */
    fun selectYear(year: Int) {
        _uiState.update { it.copy(selectedYear = year) }
    }

    /** Thêm một ngày được chọn vào danh sách thống kê */
    fun addSelectedDay(timestamp: Long) {
        val cal = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val dayStart = cal.timeInMillis
        _uiState.update { state ->
            if (!state.selectedDays.contains(dayStart)) {
                state.copy(selectedDays = (state.selectedDays + dayStart).sorted())
            } else state
        }
    }

    /** Xóa một ngày khỏi danh sách thống kê */
    fun removeSelectedDay(timestamp: Long) {
        _uiState.update { state ->
            state.copy(selectedDays = state.selectedDays.filter { it != timestamp })
        }
    }

    /** Thêm một tuần được chọn vào danh sách thống kê */
    fun addSelectedWeek(timestamp: Long) {
        val cal = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val weekStart = cal.timeInMillis
        _uiState.update { state ->
            if (!state.selectedWeeks.contains(weekStart)) {
                state.copy(selectedWeeks = (state.selectedWeeks + weekStart).sorted())
            } else state
        }
    }

    /** Xóa một tuần khỏi danh sách thống kê */
    fun removeSelectedWeek(timestamp: Long) {
        _uiState.update { state ->
            state.copy(selectedWeeks = state.selectedWeeks.filter { it != timestamp })
        }
    }

    /** Thêm một tháng được chọn vào danh sách thống kê */
    fun addSelectedMonth(month: Int, year: Int) {
        _uiState.update { state ->
            val pair = Pair(month, year)
            if (!state.selectedMonths.contains(pair)) {
                val newList = (state.selectedMonths + pair).sortedWith(compareBy<Pair<Int, Int>> { it.second }.thenBy { it.first })
                state.copy(selectedMonths = newList)
            } else state
        }
    }

    /** Xóa một tháng khỏi danh sách thống kê */
    fun removeSelectedMonth(month: Int, year: Int) {
        _uiState.update { state ->
            state.copy(selectedMonths = state.selectedMonths.filter { it != Pair(month, year) })
        }
    }

    /** Thêm một năm được chọn vào danh sách thống kê */
    fun addSelectedYear(year: Int) {
        _uiState.update { state ->
            if (!state.selectedYears.contains(year)) {
                state.copy(selectedYears = (state.selectedYears + year).sorted())
            } else state
        }
    }

    /** Xóa một năm khỏi danh sách thống kê */
    fun removeSelectedYear(year: Int) {
        _uiState.update { state ->
            state.copy(selectedYears = state.selectedYears.filter { it != year })
        }
    }
}
