/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.screen.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewWrapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import net.newpipe.app.composable.about.LibraryListItem
import net.newpipe.app.model.Library
import net.newpipe.app.preview.LibraryPreviewProvider
import net.newpipe.app.preview.ThemePreviewProvider
import net.newpipe.app.theme.spaceMedium
import net.newpipe.app.theme.spaceXSmall
import net.newpipe.app.theme.spaceXXSmall
import newpipe.composeapp.generated.resources.Res
import newpipe.composeapp.generated.resources.app_license
import newpipe.composeapp.generated.resources.app_license_title
import newpipe.composeapp.generated.resources.read_full_license
import newpipe.composeapp.generated.resources.title_licenses
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import kotlin.random.Random

@Composable
fun LicensePage(
    libraries: List<Library> = defaultLibraries(),
    onOpenUrl: (url: String) -> Unit = {}
) {
    var shouldShowLicenseDialog by rememberSaveable { mutableStateOf<Library?>(null) }
    shouldShowLicenseDialog?.let { library ->
        LicenseDialog(
            library = library,
            onOpenWebsite = { onOpenUrl(library.website!!) },
            onDismiss = { shouldShowLicenseDialog = null }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = WindowInsets.navigationBars.asPaddingValues(),
        verticalArrangement = Arrangement.spacedBy(spaceXXSmall)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spaceMedium),
                verticalArrangement = Arrangement.spacedBy(spaceXSmall)
            ) {
                Text(
                    text = stringResource(Res.string.app_license_title),
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = stringResource(Res.string.app_license),
                    style = MaterialTheme.typography.bodyMedium
                )

                TextButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentWidth(Alignment.End),
                    onClick = {}
                ) {
                    Text(text = stringResource(Res.string.read_full_license))
                }

                Text(
                    text = stringResource(Res.string.title_licenses),
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }

        // Third-party libraries and licenses
        items(items = libraries, key = { library -> library.id }) { library ->
            LibraryListItem(
                library = library,
                onClick = { shouldShowLicenseDialog = library }
            )
        }
    }
}

/**
 * Loads and serializes libraries JSON file
 */
@Composable
private fun defaultLibraries(json: Json = koinInject()): List<Library> {
    return produceState(initialValue = emptyList()) {
        val jsonString = withContext(Dispatchers.Default) {
            Res.readBytes("files/aboutlibraries.json").decodeToString()
        }
        val jsonElement = json.parseToJsonElement(jsonString)
        value = json.decodeFromString<List<Library>>(jsonElement.jsonObject["libraries"].toString())
    }.value
}

@PreviewWrapper(ThemePreviewProvider::class)
@Preview(showBackground = true)
@Composable
private fun LicensePagePreview(
    @PreviewParameter(LibraryPreviewProvider::class) library: Library
) {
    val libraries = List(10) {
        library.copy(id = Random.nextInt().toString())
    }
    LicensePage(libraries = libraries)
}
