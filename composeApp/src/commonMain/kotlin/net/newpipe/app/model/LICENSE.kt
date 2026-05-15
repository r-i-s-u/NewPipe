/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.model

/**
 * Class representing licenses used by various libraries in this app
 * @property spdxId SPDX identifier for the license
 * @property commonNames Common names for the license
 */
enum class LICENSE(val spdxId: String, val commonNames: List<String>) {
    UNAVAILABLE(spdxId = "NA", commonNames = emptyList()),
    APACHE_2_0(
        spdxId = "Apache-2.0",
        commonNames = listOf("Apache 2.0", "Apache License 2.0", "Apache")
    ),
    EPL_1_0(
        spdxId = "EPL-1.0",
        commonNames = listOf("Eclipse Public License 1.0", "EPL")
    ),
    GPL_3_0_ONLY(
        spdxId = "GPL-3.0-only",
        commonNames = listOf("GPL 3.0", "GNU GPLv3", "GPLv3")
    ),
    GPL_3_0_OR_LATER(
        spdxId = "GPL-3.0-or-later",
        commonNames = listOf("GNU GENERAL PUBLIC LICENSE, Version 3", "GPL 3.0 or later", "GPLv3+")
    ),
    MIT(
        spdxId = "MIT",
        commonNames = listOf("MIT License", "The MIT License")
    ),
    MPL_2_0(
        spdxId = "MPL-2.0",
        commonNames = listOf("Mozilla Public License 2.0", "MPL")
    )
}
