package com.rankly.eboghost

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                MainScreen(
                    hasOverlayPermission = Settings.canDrawOverlays(this),
                    onRequestOverlay = { requestOverlayPermission() },
                    onLaunchOverlay = { startOverlayService() }
                )
            }
        }
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        overlayPermissionLauncher.launch(intent)
    }

    private fun startOverlayService() {
        val intent = Intent(this, com.rankly.eboghost.overlay.OverlayService::class.java)
        startForegroundService(intent)
    }
}

@Composable
fun MainScreen(
    hasOverlayPermission: Boolean,
    onRequestOverlay: () -> Unit,
    onLaunchOverlay: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("EBO Ghost", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        if (!hasOverlayPermission) {
            Text("Overlay permission required")
            Spacer(Modifier.height(8.dp))
            Button(onClick = onRequestOverlay) { Text("Grant Permission") }
        } else {
            Text("Ready to launch overlay")
            Spacer(Modifier.height(8.dp))
            Button(onClick = onLaunchOverlay) { Text("Start EBO Ghost Overlay") }
        }
    }
}
