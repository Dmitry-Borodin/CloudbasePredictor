package com.cloudbasepredictor.ui.screens.map

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.cloudbasepredictor.model.SavedPlace
import com.cloudbasepredictor.ui.theme.CloudbasePredictorTheme
import org.junit.Rule
import org.junit.Test

class MapScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun mapScreen_autoOpensFavoritesDialogWhenAtLeastTwoFavoritesExist() {
        composeRule.setContent {
            CloudbasePredictorTheme {
                MapScreen(
                    uiState = MapUiState(favoritePlaces = favoritePlaces),
                    onMapTapped = { _, _ -> },
                    onFavoriteTapped = {},
                    onOpenForecast = {},
                    onFavoriteClick = {},
                    onSaveCameraPosition = { _, _, _ -> },
                    autoOpenFavoritesOnStartup = true,
                )
            }
        }

        composeRule.onNodeWithText("Favorites").assertIsDisplayed()
    }

    private companion object {
        val favoritePlaces = listOf(
            SavedPlace(
                id = "favorite-interlaken",
                name = "Interlaken",
                latitude = 46.5582,
                longitude = 7.8354,
                isFavorite = true,
            ),
            SavedPlace(
                id = "favorite-zurich",
                name = "Zurich",
                latitude = 47.3769,
                longitude = 8.5417,
                isFavorite = true,
            ),
        )
    }
}
