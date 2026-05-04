/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.composable.about

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import net.newpipe.app.model.Link
import net.newpipe.app.preview.ThemePreviewProvider
import net.newpipe.app.theme.spaceXSmall
import newpipe.composeapp.generated.resources.Res
import newpipe.composeapp.generated.resources.contribution_encouragement
import newpipe.composeapp.generated.resources.contribution_title
import newpipe.composeapp.generated.resources.view_on_github
import org.jetbrains.compose.resources.stringResource

/**
 * Composable to display item providing information about NewPipe
 * @param link A link item with information
 * @param onAction Callback when the action button is clicked
 */
@Composable
fun LinkListItem(modifier: Modifier = Modifier, link: Link, onAction: () -> Unit = {}) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(spaceXSmall)
    ) {
        Text(
            text = stringResource(link.title),
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = stringResource(link.description),
            style = MaterialTheme.typography.bodyMedium
        )

        TextButton(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentWidth(Alignment.End),
            onClick = onAction
        ) {
            Text(text = stringResource(link.action))
        }
    }
}

@PreviewWrapper(ThemePreviewProvider::class)
@Preview(showBackground = true)
@Composable
private fun LinkListItemPreview() {
    LinkListItem(
        link = Link(
            Res.string.contribution_title,
            Res.string.contribution_encouragement,
            Res.string.view_on_github,
            ""
        )
    )
}
