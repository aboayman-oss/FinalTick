package com.aboayman.finaltick

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity

/**
 * Hosts the new Compose-based SettingsScreen.
 * All legacy XML-based logic has been removed.
 */
class SettingsActivity : AppCompatActivity() {

    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { FinalTickTheme { SettingsScreen(viewModel) } }
    }
}
