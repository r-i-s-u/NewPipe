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
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.util.fastForEachIndexed
import kotlinx.coroutines.launch
import net.newpipe.app.composable.TopAppBar
import net.newpipe.app.model.currentServiceScheme
import net.newpipe.app.platform.ShareHandler
import net.newpipe.app.preview.ThemePreviewProvider
import org.jetbrains.compose.resources.stringResource
import net.newpipe.app.screen.about.navigation.Page
import newpipe.composeapp.generated.resources.Res
import newpipe.composeapp.generated.resources.title_activity_about
import org.koin.compose.koinInject

@Composable
fun AboutScreen(
    onNavigateUp: () -> Unit,
    shareHandler: ShareHandler = koinInject()
) {
    ScreenContent(
        onNavigateUp = onNavigateUp,
        onOpenUrl = { url -> shareHandler.openUrlInBrowser(url) }
    )
}

@Composable
private fun ScreenContent(
    pages: List<Page> = listOf(Page.ABOUT, Page.LICENSE),
    onNavigateUp: () -> Unit = {},
    onOpenUrl: (url: String) -> Unit = {},
    serviceScheme: ColorScheme = currentServiceScheme()
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
                .padding(top = paddingValues.calculateTopPadding())
        ) {
            val pagerState = rememberPagerState { pages.size }
            val coroutineScope = rememberCoroutineScope()

            SecondaryTabRow(
                modifier = Modifier.fillMaxWidth(),
                containerColor = serviceScheme.primaryContainer,
                indicator = {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(
                            selectedTabIndex = pagerState.currentPage,
                            matchContentSize = false
                        ),
                        color = serviceScheme.onPrimaryContainer
                    )
                },
                selectedTabIndex = pagerState.currentPage
            ) {
                pages.fastForEachIndexed { index, _ ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        text = {
                            Text(
                                text = stringResource(pages[index].localizedTitle),
                                color = serviceScheme.onPrimaryContainer
                            )
                        },
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        }
                    )
                }
            }

            HorizontalPager(modifier = Modifier.fillMaxSize(), state = pagerState,) { page ->
                when (pages[page]) {
                    Page.ABOUT -> AboutPage(onOpenUrl = onOpenUrl)
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
