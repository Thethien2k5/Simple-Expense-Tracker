package com.T2V.simple_expense_tracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxWidth
 import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.hilt.navigation.compose.hiltViewModel
import com.T2V.simple_expense_tracker.domain.repository.ThemeRepository
import com.T2V.simple_expense_tracker.ui.dashboard.DashboardScreen
import com.T2V.simple_expense_tracker.ui.ledger.NotificationPanel
import com.T2V.simple_expense_tracker.ui.settings.SettingsPanel
import com.T2V.simple_expense_tracker.ui.theme.AppTheme
import com.T2V.simple_expense_tracker.ui.theme.SimpleExpenseTrackerTheme
import com.T2V.simple_expense_tracker.ui.theme.SurfaceContainerHigh
import com.T2V.simple_expense_tracker.ui.theme.LocalAppStrings
import com.T2V.simple_expense_tracker.ui.theme.getAppStringsForLanguage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import com.T2V.simple_expense_tracker.ui.notification.NotificationActionViewModel
import kotlinx.coroutines.flow.first
import com.T2V.simple_expense_tracker.util.LocaleHelper
import  com.T2V.simple_expense_tracker.domain.repository.LanguageRepository

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var themeRepository: ThemeRepository
    @Inject
    lateinit var languageRepository: LanguageRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Áp dụng ngôn ngữ đã lưu trước khi setContent
        kotlinx.coroutines.runBlocking {
            val lang = languageRepository.selectedLanguage.first()
            LocaleHelper.setLocale(this@MainActivity, lang.code)
        }

        enableEdgeToEdge()
        tryRebindNotificationListener()
        setContent {
            val theme = themeRepository.selectedTheme.collectAsState(initial = AppTheme.EMERALD).value
            val currentLanguage = languageRepository.selectedLanguage.collectAsState(initial = com.T2V.simple_expense_tracker.domain.repository.AppLanguage.VIETNAMESE).value
            val appStrings = getAppStringsForLanguage(currentLanguage)

            SimpleExpenseTrackerTheme(theme = theme) {
                CompositionLocalProvider(LocalAppStrings provides appStrings) {
                    MainApp()
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun tryRebindNotificationListener() {
        try {
            val component = android.content.ComponentName(this, com.T2V.simple_expense_tracker.service.BankNotificationListenerService::class.java)
            val pm = packageManager
            pm.setComponentEnabledSetting(
                component,
                android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                android.content.pm.PackageManager.DONT_KILL_APP
            )
            pm.setComponentEnabledSetting(
                component,
                android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                android.content.pm.PackageManager.DONT_KILL_APP
            )
            android.util.Log.d("MainActivity", "Đã tự động kích hoạt kết nối dịch vụ nghe thông báo (Rebind Service)")
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Lỗi rebind NotificationListenerService: ${e.message}", e)
        }
    }
}

@Composable
fun MainApp(
    notificationActionViewModel: NotificationActionViewModel = hiltViewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var showPermissionDialog by remember { mutableStateOf(false) }

    // Theo dõi Intent thay đổi từ Activity (khi nhận thông báo)
    val activity = context as? android.app.Activity
    LaunchedEffect(activity?.intent) {
        activity?.intent?.let { notificationActionViewModel.handleIntent(it) }
    }

    val anomalyState by notificationActionViewModel.anomalyUiState.collectAsState()
    val manualParseState by notificationActionViewModel.manualParseUiState.collectAsState()

    // Hiển thị Dialog cảnh báo bất thường số dư
    if (anomalyState.show) {
        com.T2V.simple_expense_tracker.ui.notification.AnomalyConfirmationDialog(
            bankName = anomalyState.bankName,
            currentBalance = anomalyState.currentBalance,
            transactionAmount = anomalyState.transactionAmount,
            expectedBalance = anomalyState.expectedBalance,
            reportedBalance = anomalyState.reportedBalance,
            onConfirm = { notificationActionViewModel.confirmAnomaly() },
            onDismiss = { notificationActionViewModel.rejectAnomaly() }
        )
    }

    // Hiển thị Màn hình nhập giao dịch thủ công
    if (manualParseState.show) {
        com.T2V.simple_expense_tracker.ui.notification.ManualParseScreen(
            state = manualParseState,
            onStateChange = { amountText, isCredit, accountNumber, content, counterparty ->
                notificationActionViewModel.updateManualParseState(
                    amountText, isCredit, accountNumber, content, counterparty
                )
            },
            onSave = { notificationActionViewModel.saveManualParse() },
            onDismiss = { notificationActionViewModel.dismissManualParseScreen() }
        )
    }

    fun checkPermission() {
        val flat = android.provider.Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        val isGranted = if (!flat.isNullOrBlank()) {
            flat.split(":").any { name ->
                val cn = android.content.ComponentName.unflattenFromString(name)
                cn != null && cn.packageName == context.packageName
            }
        } else false
        showPermissionDialog = !isGranted
    }

    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                checkPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { /* Bắt buộc không cho đóng */ },
            title = {
                Text(
                    text = "Yêu cầu quyền truy cập thông báo ngầm",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "Để ứng dụng có thể tự động ghi nhận giao dịch thu chi từ thông báo biến động số dư các ngân hàng, bạn cần cấp quyền đọc thông báo ngầm cho hệ thống.\n\nĐây là quyền bắt buộc để ứng dụng hoạt động chính xác.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val intent = android.content.Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS").apply {
                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    }
                ) {
                    Text("Cấp quyền ngay", color = MaterialTheme.colorScheme.onPrimary)
                }
            },
            dismissButton = null,
            shape = RoundedCornerShape(28.dp),
            containerColor = SurfaceContainerHigh
        )
    }

    // Left drawer state (Settings)
    val leftDrawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    // Right drawer state (Notifications)
    val rightDrawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    
    val scope = rememberCoroutineScope()

    // Right Drawer (Notifications) wraps the Left Drawer
    // Trick: Thay đổi LayoutDirection thành RTL để ModalNavigationDrawer hiển thị từ bên phải
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ModalNavigationDrawer(
            drawerState = rightDrawerState,
            drawerContent = {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    ModalDrawerSheet(modifier = Modifier.fillMaxWidth(0.85f)) {
                        NotificationPanel(
                            viewModel = hiltViewModel(),
                            onMenuClick = { scope.launch { rightDrawerState.close() } }
                        )
                    }
                }
            }
        ) {
            // Đặt lại LayoutDirection thành LTR cho nội dung bên trong
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                // Left Drawer (Settings) wraps Main Content
                ModalNavigationDrawer(
                    drawerState = leftDrawerState,
                    drawerContent = {
                        ModalDrawerSheet(modifier = Modifier.fillMaxWidth(0.85f)) {
                            SettingsPanel(
                                viewModel = hiltViewModel(),
                                onMenuClick = { scope.launch { leftDrawerState.close() } }
                            )
                        }
                    }
                ) {
                    // Main Content (Dashboard)
                    DashboardScreen(
                        viewModel = hiltViewModel(),
                        onMenuClick = { scope.launch { leftDrawerState.open() } },
                        onNotificationClick = { scope.launch { rightDrawerState.open() } }
                    )
                }
            }
        }
    }
}