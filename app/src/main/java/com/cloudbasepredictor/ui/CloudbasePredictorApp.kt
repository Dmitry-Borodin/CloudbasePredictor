package com.cloudbasepredictor.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
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
