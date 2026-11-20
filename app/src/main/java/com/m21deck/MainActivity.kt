package com.m21deck

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.m21deck.ui.DeckApp
import com.m21deck.ui.DeckState
import com.m21deck.ui.DeckTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val scope = rememberCoroutineScope()
            val state = remember { DeckState(applicationContext, scope) }
            var granted by remember {
                mutableStateOf(
                    ContextCompat.checkSelfPermission(this, if (Build.VERSION.SDK_INT >= 33)
                        Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                )
            }
            val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
            val permissionName = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
                else Manifest.permission.READ_EXTERNAL_STORAGE
            DisposableEffect(state) { onDispose { state.close() } }
            LaunchedEffect(granted) { if (granted) state.refreshLibrary() }
            LaunchedEffect(state) { while (true) { state.player.tick(); delay(500) } }
            DeckTheme {
                if (granted) DeckApp(state) else Surface(modifier = Modifier.fillMaxSize()) {
                    Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("M21 Control Deck", style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.height(12.dp))
                        Text("Allow music access to scan the M21 and microSD library. Your music stays on this device.")
                        Spacer(Modifier.height(18.dp))
                        Button(onClick = { permission.launch(permissionName) }) { Text("Allow music access") }
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(onClick = { granted = true }) { Text("Use launcher without scanning") }
                    }
                }
            }
        }
    }
}
