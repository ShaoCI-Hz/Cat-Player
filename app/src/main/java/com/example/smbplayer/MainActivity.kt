package com.example.smbplayer

import android.os.Build
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.smbplayer.ui.navigation.SmbPlayerAppContent
import com.example.smbplayer.ui.settings.SettingsViewModel
import com.example.smbplayer.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestHighFrameRate()
        setContent {
            val settingsVM: SettingsViewModel = hiltViewModel()
            val themeMode by settingsVM.themeMode.collectAsState()

            AppTheme(themeMode = themeMode) {
                SmbPlayerAppContent()
            }
        }
    }

    private fun requestHighFrameRate() {
        // Request highest available frame rate for smooth animations
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.attributes = window.attributes.apply {
                preferredDisplayModeId = display?.let { display ->
                    val modes = display.supportedModes
                    modes.maxByOrNull { it.refreshRate }?.modeId ?: 0
                } ?: 0
            }
        }
        // Also set via WindowManager for older devices
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}
