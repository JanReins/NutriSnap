package com.janreins.nutrisnap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.janreins.nutrisnap.ui.navigation.NutriNavGraph
import com.janreins.nutrisnap.ui.theme.NutriSnapTheme
import com.janreins.nutrisnap.ui.viewmodel.NutriViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: NutriViewModel by viewModels()
    private var lastBackgroundTimeMillis: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
            NutriSnapTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NutriNavGraph(viewModel = viewModel)
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        lastBackgroundTimeMillis = System.currentTimeMillis()
    }

    override fun onStart() {
        super.onStart()
        if (lastBackgroundTimeMillis > 0) {
            viewModel.checkBackgroundTimeoutAndLock(lastBackgroundTimeMillis)
        }
    }
}
