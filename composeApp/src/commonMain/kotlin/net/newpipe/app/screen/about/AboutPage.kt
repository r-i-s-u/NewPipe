/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.screen.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import net.newpipe.app.Constants
import net.newpipe.app.composable.about.LinkListItem
import net.newpipe.app.model.Link
import net.newpipe.app.preview.ThemePreviewProvider
import net.newpipe.app.theme.iconTVDPI
import net.newpipe.app.theme.logoBackground
import net.newpipe.app.theme.spaceLarge
import net.newpipe.app.theme.spaceNormal
import net.newpipe.app.theme.spaceXSmall
import net.newpipe.app.theme.spaceXXSmall
import newpipe.composeapp.generated.resources.Res
import newpipe.composeapp.generated.resources.app_description
import newpipe.composeapp.generated.resources.app_name
import newpipe.composeapp.generated.resources.contribution_encouragement
import newpipe.composeapp.generated.resources.contribution_title
import newpipe.composeapp.generated.resources.donation_encouragement
import newpipe.composeapp.generated.resources.donation_title
import newpipe.composeapp.generated.resources.faq
import newpipe.composeapp.generated.resources.faq_description
import newpipe.composeapp.generated.resources.faq_title
import newpipe.composeapp.generated.resources.give_back
import newpipe.composeapp.generated.resources.ic_foreground
import newpipe.composeapp.generated.resources.open_in_browser
import newpipe.composeapp.generated.resources.privacy_policy_encouragement
import newpipe.composeapp.generated.resources.privacy_policy_title
import newpipe.composeapp.generated.resources.read_privacy_policy
import newpipe.composeapp.generated.resources.view_on_github
import newpipe.composeapp.generated.resources.website_encouragement
import newpipe.composeapp.generated.resources.website_title
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private val DEFAULT_LINKS = listOf(
    Link(
        title = Res.string.faq_title,
        description = Res.string.faq_description,
        action = Res.string.faq,
        url = Constants.URL_FAQ
    ),
    Link(
        title = Res.string.contribution_title,
        description = Res.string.contribution_encouragement,
        action = Res.string.view_on_github,
        url = Constants.URL_GITHUB
    ),
    Link(
        title = Res.string.donation_title,
        description = Res.string.donation_encouragement,
        action = Res.string.give_back,
        url = Constants.URL_DONATION
    ),
    Link(
        title = Res.string.website_title,
        description = Res.string.website_encouragement,
        action = Res.string.open_in_browser,
        url = Constants.URL_WEBSITE
    ),
    Link(
        title = Res.string.privacy_policy_title,
        description = Res.string.privacy_policy_encouragement,
        action = Res.string.read_privacy_policy,
        url = Constants.URL_PRIVACY
    )
)

@Composable
fun AboutPage(links: List<Link> = DEFAULT_LINKS) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(spaceNormal),
        verticalArrangement = Arrangement.spacedBy(spaceXXSmall)
    ) {
        // Page Header
        item {
            Column(
                modifier = Modifier
                    .padding(spaceLarge)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    modifier = Modifier
                        .requiredSize(iconTVDPI)
                        .clip(CircleShape)
                        .background(color = logoBackground),
                    painter = painterResource(Res.drawable.ic_foreground),
                    contentDescription = stringResource(Res.string.app_name),
                )
                Spacer(modifier = Modifier.height(spaceXSmall))
                Text(
                    text = stringResource(Res.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = Constants.CODE_VERSION,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(Res.string.app_description),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Links about NewPipe
        items(items = links, key = { link -> link.url }) { link ->
            LinkListItem(
                link = link
            )
        }
    }
}

@PreviewWrapper(ThemePreviewProvider::class)
@Preview(showBackground = true)
@Composable
private fun AboutPagePreview() {
    AboutPage()
}
