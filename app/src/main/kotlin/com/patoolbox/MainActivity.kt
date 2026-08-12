package com.patoolbox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patoolbox.core.designsystem.theme.PaTheme
import com.patoolbox.navigation.PaNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // targetSdk 35 以降はエッジツーエッジが強制されるので、明示的に有効化して
        // インセットは Scaffold 側で処理する
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

            PaTheme(themeMode = themeMode) {
                PaNavHost()
            }
        }
    }
}
