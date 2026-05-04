/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.screen.about

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.util.fastForEachIndexed
import kotlinx.coroutines.launch
import net.newpipe.app.composable.TopAppBar
import net.newpipe.app.preview.ThemePreviewProvider
import org.jetbrains.compose.resources.stringResource
import net.newpipe.app.screen.about.navigation.Page
import newpipe.composeapp.generated.resources.Res
import newpipe.composeapp.generated.resources.title_activity_about

@Composable
fun AboutScreen(onNavigateUp: () -> Unit) {
    ScreenContent(
        onNavigateUp = onNavigateUp
    )
}

@Composable
private fun ScreenContent(
    pages: List<Page> = listOf(Page.ABOUT, Page.LICENSE),
    onNavigateUp: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = stringResource(Res.string.title_activity_about),
                onNavigateUp = onNavigateUp
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val pagerState = rememberPagerState { pages.size }
            val coroutineScope = rememberCoroutineScope()

            SecondaryTabRow(
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedTabIndex = pagerState.currentPage
            ) {
                pages.fastForEachIndexed { index, _ ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        text = { Text(text = stringResource(pages[index].localizedTitle)) },
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        }
                    )
                }
            }

            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                when (pages[page]) {
                    Page.ABOUT -> AboutPage()
                    Page.LICENSE -> LicensePage()
                }
            }
        }
    }
}

@PreviewWrapper(ThemePreviewProvider::class)
@Preview
@Composable
private fun AboutScreenPreview() {
    ScreenContent()
}
