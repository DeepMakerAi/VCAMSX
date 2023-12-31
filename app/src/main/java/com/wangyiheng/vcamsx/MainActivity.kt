package com.wangyiheng.vcamsx

import HomeScreen
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.wangyiheng.vcamsx.services.VcamsxForegroundService
import com.wangyiheng.vcamsx.ui.theme.VCAMSXTheme

class MainActivity : ComponentActivity() {

    private val notificationSettingsResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (areNotificationsEnabled()) {
            startForegroundService()
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        if (areNotificationsEnabled()) {
//            startForegroundService()
//        } else {
//            openNotificationSettings()
//        }
        setContent {
            VCAMSXTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background) {
                    HomeScreen()
                }
            }
        }
    }

    private fun openNotificationSettings() {
        val intent = Intent().apply {
            action = "android.settings.APP_NOTIFICATION_SETTINGS"
            putExtra("android.provider.extra.APP_PACKAGE", packageName)
            putExtra("app_package", packageName)
            putExtra("app_uid", applicationInfo.uid)
        }
        notificationSettingsResult.launch(intent)
    }
    private fun startForegroundService() {
        VcamsxForegroundService.start(this)
    }

    private fun areNotificationsEnabled(): Boolean {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return notificationManager.areNotificationsEnabled()
    }

}