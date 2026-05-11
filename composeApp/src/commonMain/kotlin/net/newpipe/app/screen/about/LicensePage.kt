/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.screen.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.ui.compose.LibraryDefaults
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import com.mikepenz.aboutlibraries.ui.compose.produceLibraries
import net.newpipe.app.Constants
import net.newpipe.app.composable.about.LinkListItem
import net.newpipe.app.model.Link
import net.newpipe.app.preview.ThemePreviewProvider
import net.newpipe.app.theme.spaceXSmall
import newpipe.composeapp.generated.resources.Res
import newpipe.composeapp.generated.resources.app_license
import newpipe.composeapp.generated.resources.app_license_title
import newpipe.composeapp.generated.resources.read_full_license
import newpipe.composeapp.generated.resources.title_licenses
import org.jetbrains.compose.resources.stringResource

private val DEFAULT_LIBRARIES: Libs?
    @Composable
    get() = produceLibraries {
        Res.readBytes("files/aboutlibraries.json").decodeToString()
    }.value

@Composable
fun LicensePage(libs: Libs? = DEFAULT_LIBRARIES) {
    LibrariesContainer(
        modifier = Modifier.fillMaxSize(),
        contentPadding = WindowInsets.navigationBars.asPaddingValues(),
        showDescription = true,
        libraries = libs,
        header = {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(LibraryDefaults.libraryPadding().contentPadding),
                    verticalArrangement = Arrangement.spacedBy(spaceXSmall)
                ) {
                    LinkListItem(
                        link = Link(
                            title = Res.string.app_license_title,
                            description = Res.string.app_license,
                            action = Res.string.read_full_license,
                            url = Constants.URL_LICENSE
                        )
                    )

                    Text(
                        text = stringResource(Res.string.title_licenses),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    )
}

@PreviewWrapper(ThemePreviewProvider::class)
@Preview(showBackground = true)
@Composable
private fun LicensePagePreview() {
    LicensePage()
}
