package com.cloudbasepredictor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.cloudbasepredictor.data.local.DatabaseErrorManager
import com.cloudbasepredictor.data.theme.ThemeRepository
import com.cloudbasepredictor.ui.CloudbasePredictorApp
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var databaseErrorManager: DatabaseErrorManager

    @Inject
    lateinit var themeRepository: ThemeRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            CloudbasePredictorApp(
                databaseErrorManager = databaseErrorManager,
                themeRepository = themeRepository,
            )
        }
    }
}
