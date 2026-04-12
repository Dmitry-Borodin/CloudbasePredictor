package com.cloudbasepredictor.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.cloudbasepredictor.ui.navigation.CloudbaseNavGraph
import com.cloudbasepredictor.ui.theme.CloudbasePredictorTheme

@Composable
fun CloudbasePredictorApp(
    navGraph: @Composable (Modifier, NavHostController) -> Unit = { modifier, navController ->
        CloudbaseNavGraph(
            navController = navController,
            modifier = modifier,
        )
    },
) {
    CloudbasePredictorTheme {
        val navController = rememberNavController()
        navGraph(Modifier.fillMaxSize(), navController)
    }
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
