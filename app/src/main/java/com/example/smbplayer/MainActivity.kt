package com.example.smbplayer

import android.os.Bundle
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
        setContent {
            val settingsVM: SettingsViewModel = hiltViewModel()
            val themeMode by settingsVM.themeMode.collectAsState()

            AppTheme(themeMode = themeMode) {
                SmbPlayerAppContent()
            }
        }
    }
}
