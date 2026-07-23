package com.hematoscope.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.hematoscope.app.ui.navigation.HematoNavHost
import com.hematoscope.app.ui.theme.HematoScopeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HematoScopeTheme {
                HematoNavHost()
            }
        }
    }
}
