/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.composable.about

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import net.newpipe.app.model.Link
import newpipe.composeapp.generated.resources.Res
import newpipe.composeapp.generated.resources.contribution_encouragement
import newpipe.composeapp.generated.resources.contribution_title
import newpipe.composeapp.generated.resources.view_on_github
import org.jetbrains.compose.resources.getString
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class LinkListItemTest {

    @Test
    fun testLinkListItem() = runComposeUiTest {
        var actionClicked = false
        setContent {
            LinkListItem(
                link = Link(
                    Res.string.contribution_title,
                    Res.string.contribution_encouragement,
                    Res.string.view_on_github,
                    ""
                ),
                onAction = { actionClicked = true }
            )
        }

        onNodeWithText(getString(Res.string.contribution_title)).isDisplayed()
        onNodeWithText(getString(Res.string.contribution_encouragement)).isDisplayed()
        onNodeWithText(getString(Res.string.view_on_github)).apply {
            isDisplayed()
            performClick()
            assertTrue(actionClicked)
        }
    }
}
