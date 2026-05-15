/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.screen.about

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewWrapper
import net.newpipe.app.model.Library
import net.newpipe.app.preview.LibraryPreviewProvider
import net.newpipe.app.preview.ThemePreviewProvider
import newpipe.composeapp.generated.resources.Res
import newpipe.composeapp.generated.resources.done
import newpipe.composeapp.generated.resources.website_title
import org.jetbrains.compose.resources.stringResource

/**
 * Dialog to show license and other details for a library
 * @param modifier Modifier for the dialog
 * @param library Library to show the details and license
 * @param onOpenWebsite Callback when action button to view library's website is clicked
 * @param onDismiss Callback when the dialog is dismissed
 */
@Composable
fun LicenseDialog(
    modifier: Modifier = Modifier,
    library: Library,
    onOpenWebsite: () -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    var licenseBytes by remember { mutableStateOf(byteArrayOf(0)) }
    LaunchedEffect(key1 = Unit) {
        licenseBytes = Res.readBytes("files/LICENSES/${library.primaryLicense.spdxId}.txt")
    }

    AlertDialog(
        modifier = modifier,
        title = { Text(text = library.name) },
        text = {
            Text(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                text = licenseBytes.decodeToString()
            )
        },
        onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(onClick = onOpenWebsite, enabled = library.website != null) {
                Text(text = stringResource(Res.string.website_title))
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(Res.string.done))
            }
        }
    )
}

@PreviewWrapper(ThemePreviewProvider::class)
@Preview(showBackground = true)
@Composable
private fun LicenseDialogPreview(
    @PreviewParameter(LibraryPreviewProvider::class) library: Library
) {
    LicenseDialog(library = library)
}
