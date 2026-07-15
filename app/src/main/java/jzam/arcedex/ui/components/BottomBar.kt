package jzam.arcedex.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import jzam.arcedex.R
import jzam.arcedex.models.SupportedLanguage
import jzam.arcedex.utils.formatSearchedText

//App's bottom bar - used for searching the Pokemon list
@Composable
fun ArcedexBottomBar(
    language: SupportedLanguage,
    inSearchMode: Boolean, searchText: String, onSearch: (String) -> Unit,
    searchClear: () -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 8.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!inSearchMode) {
                Text(
                    formatSearchedText(language, searchText),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(
                    onClick = { searchClear() },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Text(stringResource(R.string.clear_label), style = MaterialTheme.typography.labelLarge)
                }
            } else {
                SearchInputText(onSearch)
            }
        }
    }
}

//Search bar and button
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun RowScope.SearchInputText(
    onDone: (String) -> Unit = {}
) {
    var text by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    TextField(
        value = text,
        onValueChange = { text = it },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedIndicatorColor = MaterialTheme.colorScheme.primary,
            unfocusedIndicatorColor = MaterialTheme.colorScheme.outline
        ),
        maxLines = 1,
        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = {
            onDone(text)
            keyboardController?.hide()
        }),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .weight(1f)
            .padding(end = 8.dp),
        label = { Text(stringResource(R.string.search_label)) }
    )
    Button(
        onClick = { onDone(text) },
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Text(stringResource(R.string.done_label), style = MaterialTheme.typography.labelLarge)
    }
}
