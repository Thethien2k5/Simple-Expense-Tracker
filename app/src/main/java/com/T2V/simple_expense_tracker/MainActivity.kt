package com.T2V.simple_expense_tracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.T2V.simple_expense_tracker.domain.repository.ThemeRepository
import com.T2V.simple_expense_tracker.ui.dashboard.DashboardScreen
import com.T2V.simple_expense_tracker.ui.ledger.NotificationPanel
import com.T2V.simple_expense_tracker.ui.settings.SettingsPanel
import com.T2V.simple_expense_tracker.ui.theme.AppTheme
import com.T2V.simple_expense_tracker.ui.theme.SimpleExpenseTrackerTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
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
        setContent {
            val theme = themeRepository.selectedTheme.collectAsState(initial = AppTheme.EMERALD).value
            SimpleExpenseTrackerTheme(theme = theme) {
                MainApp()
            }
        }
    }
}

@Composable
fun MainApp() {
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