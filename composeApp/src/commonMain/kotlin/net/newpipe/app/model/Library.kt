/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Model class to hold data for a library used in this app
 */
@Serializable
data class Library(
    @SerialName("uniqueId")
    val id: String,

    @SerialName("artifactVersion")
    val version: String? = null,

    val name: String,
    val description: String? = null,
    val website: String? = null,
    val copyright: String? = null,

    private val developers: Set<Map<String, String>> = emptySet(),
    private val licenses: Set<String> = emptySet(),
) {
    val primaryDeveloper: String
        get() = developers.first().getValue("name")

    val primaryLicense: LICENSE
        get() = LICENSE.entries.find { license -> license.spdxId == licenses.first() }!!
}
