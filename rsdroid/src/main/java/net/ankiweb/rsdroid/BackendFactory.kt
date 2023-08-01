/*
 * Copyright (c) 2020 David Allison <davidallisongithub@gmail.com>
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.ankiweb.rsdroid

import android.content.Context
typealias CustomBackendCreator = (context: Context, languages: Iterable<String>) -> Backend

object BackendFactory {
    /** To remove in 2.1.67 update */
    @JvmStatic
    @Suppress("unused")
    var defaultLegacySchema: Boolean = false

    /**
     * The language(es) the backend uses for translations.
     */
    var defaultLanguages: Iterable<String> = listOf("en")

    @JvmStatic
    private var backendForTesting: CustomBackendCreator? = null

    @JvmStatic
    @JvmOverloads
    fun getBackend(context: Context, languages: Iterable<String>? = null): Backend {
        val langs = languages ?: defaultLanguages
        val legacy = false
        return backendForTesting?.invoke(context, langs) ?: Backend(
                context,
                langs,
                legacy
        )
    }

    /** Allows overriding the returned backend for unit tests */
    fun setOverride(creator: CustomBackendCreator?) {
        backendForTesting = creator
    }
}
