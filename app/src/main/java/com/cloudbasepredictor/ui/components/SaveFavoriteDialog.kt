package com.cloudbasepredictor.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cloudbasepredictor.R
import com.cloudbasepredictor.ui.theme.CloudbasePredictorTheme

@Composable
fun SaveFavoriteDialog(
    currentName: String,
    isFavorite: Boolean,
    onSave: (String) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = if (isFavorite) stringResource(R.string.dialog_title_edit_favorite) else stringResource(R.string.dialog_title_save_favorite))
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(text = stringResource(R.string.label_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (isFavorite) {
                    TextButton(onClick = {
                        onDelete()
                        onDismiss()
                    }) {
                        Text(
                            text = stringResource(R.string.action_delete),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                TextButton(
                    onClick = {
                        onSave(name.trim())
                        onDismiss()
                    },
                    enabled = name.isNotBlank(),
                ) {
                    Text(text = stringResource(R.string.action_save))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.action_cancel))
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun SaveFavoriteDialogNewPreview() {
    CloudbasePredictorTheme {
        SaveFavoriteDialog(
            currentName = "",
            isFavorite = false,
            onSave = {},
            onDelete = {},
            onDismiss = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SaveFavoriteDialogEditPreview() {
    CloudbasePredictorTheme {
        SaveFavoriteDialog(
            currentName = "Interlaken",
            isFavorite = true,
            onSave = {},
            onDelete = {},
            onDismiss = {},
        )
    }
}
