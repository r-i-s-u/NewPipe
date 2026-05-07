/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import newpipe.composeapp.generated.resources.Res
import newpipe.composeapp.generated.resources.app_name
import org.jetbrains.compose.resources.stringResource

/**
 * Entry point for compose-related UI components on Desktop
 */
fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = stringResource(Res.string.app_name)) {
        App(onCloseRequest = ::exitApplication)
    }
}
