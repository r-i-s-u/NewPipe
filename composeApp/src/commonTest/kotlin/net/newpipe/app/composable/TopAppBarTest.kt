/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.composable

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertTrue
import newpipe.composeapp.generated.resources.Res
import newpipe.composeapp.generated.resources.navigate_back
import newpipe.composeapp.generated.resources.title_activity_about
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalTestApi::class)
class TopAppBarTest {

    @Test
    fun testTopAppBarHasNoDefaultNavigation() = runComposeUiTest {
        setContent {
            TopAppBar()
        }
        onNodeWithContentDescription(getString(Res.string.navigate_back)).assertDoesNotExist()
    }

    @Test
    fun testTopAppBar() = runComposeUiTest {
        var navigationBackClicked = false
        setContent {
            TopAppBar(
                title = stringResource(Res.string.title_activity_about),
                onNavigateUp = { navigationBackClicked = true }
            )
        }

        onNodeWithText(getString(Res.string.title_activity_about)).isDisplayed()
        onNodeWithContentDescription(getString(Res.string.navigate_back)).apply {
            assertExists()
            performClick()
            assertTrue(navigationBackClicked)
        }
    }
}
