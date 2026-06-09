package com.T2V.simple_expense_tracker

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Fingerprint
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
import kotlinx.coroutines.flow.first
import com.T2V.simple_expense_tracker.util.LocaleHelper
import  com.T2V.simple_expense_tracker.domain.repository.LanguageRepository
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability

@AndroidEntryPoint
class MainActivity : androidx.fragment.app.FragmentActivity() {
    @Inject
    lateinit var themeRepository: ThemeRepository
    @Inject
    lateinit var languageRepository: LanguageRepository

    private val isDeviceUnlocked = mutableStateOf(false)
    private lateinit var appUpdateManager: AppUpdateManager
    private val updateRequestCode = 9902

    private fun showBiometricPrompt() {
        val biometricManager = BiometricManager.from(this)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        
        val canAuthenticate = biometricManager.canAuthenticate(authenticators)
        if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
            isDeviceUnlocked.value = true
            return
        }

        val executor = ContextCompat.getMainExecutor(this)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                isDeviceUnlocked.value = true
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
            }
        }

        val biometricPrompt = BiometricPrompt(this, executor, callback)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Xác thực bảo mật")
            .setSubtitle("Sử dụng vân tay hoặc mật khẩu để mở khóa ứng dụng")
            .setAllowedAuthenticators(authenticators)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    override fun onResume() {
        super.onResume()
        if (!isDeviceUnlocked.value) {
            showBiometricPrompt()
        }
        if (::appUpdateManager.isInitialized) {
            appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        AppUpdateType.IMMEDIATE,
                        this,
                        updateRequestCode
                    )
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        isDeviceUnlocked.value = false
    }

    private fun checkNewUpdateAvailability() {
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
            ) {
                appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    AppUpdateType.IMMEDIATE,
                    this,
                    updateRequestCode
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appUpdateManager = AppUpdateManagerFactory.create(this)
        checkNewUpdateAvailability()

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
                    if (isDeviceUnlocked.value) {
                        MainApp()
                    } else {
                        LockScreen(theme = theme, onUnlockClick = { showBiometricPrompt() })
                    }
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
fun MainApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var showPermissionDialog by remember { mutableStateOf(false) }

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

@Composable
fun LockScreen(
    theme: AppTheme,
    onUnlockClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Simple Expense Tracker",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Ứng dụng đang được khóa để bảo mật",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Button(
                onClick = onUnlockClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(48.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "MỞ KHÓA",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}