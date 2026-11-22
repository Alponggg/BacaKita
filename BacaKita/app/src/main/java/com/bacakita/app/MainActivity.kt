package com.bacakita.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.bacakita.app.ui.BacaKitaApp
import com.bacakita.app.ui.theme.BacaKitaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BacaKitaTheme { BacaKitaApp() }
        }
    }
}
