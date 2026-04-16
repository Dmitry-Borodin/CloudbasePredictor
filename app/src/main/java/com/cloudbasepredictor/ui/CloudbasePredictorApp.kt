package com.cloudbasepredictor.ui

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.core.view.WindowCompat
import com.cloudbasepredictor.R
import com.cloudbasepredictor.data.local.DatabaseErrorManager
import com.cloudbasepredictor.data.theme.ThemePreference
import com.cloudbasepredictor.data.theme.ThemeRepository
import com.cloudbasepredictor.ui.navigation.CloudbaseNavGraph
import com.cloudbasepredictor.ui.theme.CloudbasePredictorTheme

@Composable
fun CloudbasePredictorApp(
    databaseErrorManager: DatabaseErrorManager? = null,
    themeRepository: ThemeRepository? = null,
    navGraph: @Composable (Modifier, NavHostController) -> Unit = { modifier, navController ->
        CloudbaseNavGraph(
            navController = navController,
            modifier = modifier,
        )
    },
) {
    val themePref = themeRepository?.preference?.collectAsStateWithLifecycle()
    val darkTheme = when (themePref?.value) {
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
        else -> isSystemInDarkTheme()
    }

    CloudbasePredictorTheme(darkTheme = darkTheme) {
        ApplySystemBarsStyle(darkTheme = darkTheme)

        Surface(modifier = Modifier.fillMaxSize()) {
            val navController = rememberNavController()
            navGraph(Modifier.fillMaxSize(), navController)

            if (databaseErrorManager != null) {
                val showDbError by databaseErrorManager.showError.collectAsStateWithLifecycle()
                if (showDbError) {
                    val context = LocalContext.current
                    DatabaseErrorDialog(
                        onClearDatabase = { databaseErrorManager.clearDatabaseAndRestart() },
                        onCloseApp = {
                            (context as? android.app.Activity)?.finish()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ApplySystemBarsStyle(darkTheme: Boolean) {
    val view = LocalView.current
    val activity = view.context.findActivity() ?: return

    SideEffect {
        val window = activity.window
        val transparent = Color.Transparent.toArgb()
        window.statusBarColor = transparent
        window.navigationBarColor = transparent
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }

        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }
    }
}

private tailrec fun android.content.Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is android.content.ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}

@Composable
private fun DatabaseErrorDialog(
    onClearDatabase: () -> Unit,
    onCloseApp: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { /* non-dismissable */ },
        title = { Text(text = stringResource(R.string.db_error_title)) },
        text = { Text(text = stringResource(R.string.db_error_message)) },
        confirmButton = {
            TextButton(onClick = onClearDatabase) {
                Text(text = stringResource(R.string.db_error_clear))
            }
        },
        dismissButton = {
            TextButton(onClick = onCloseApp) {
                Text(text = stringResource(R.string.db_error_close_app))
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun CloudbasePredictorAppPreview() {
    CloudbasePredictorApp(
        navGraph = { modifier, _ ->
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "Cloudbase app preview")
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun DatabaseErrorDialogPreview() {
    CloudbasePredictorTheme {
        DatabaseErrorDialog(
            onClearDatabase = {},
            onCloseApp = {},
        )
    }
}
