/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import net.newpipe.app.model.Library

/**
 * Preview provider for composable working with [Library]
 */
class LibraryPreviewProvider : PreviewParameterProvider<Library> {

    override val values: Sequence<Library>
        get() = sequenceOf(
            Library(
                id = "net.newpipe.extractor",
                name = "NewPipe Extractor",
                version = "0.26.1",
                description = "A library for extracting data from streaming websites, used in NewPipe",
                website = "https://github.com/TeamNewPipe/NewPipeExtractor/",
                developers = setOf(mapOf("name" to "Team NewPipe")),
                licenses = setOf("GPL-3.0-or-later"),
                copyright = "2025-2026"
            )
        )
}
