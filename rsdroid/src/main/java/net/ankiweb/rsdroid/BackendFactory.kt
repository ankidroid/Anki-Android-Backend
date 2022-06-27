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
import java.util.*

typealias CustomBackendCreator = (context: Context, languages: Iterable<String>, legacySchema: Boolean) -> Backend

object BackendFactory {
    /**
     * If enabled, collections are upgraded to the latest schema version on open, and different
     * code paths are used to access the collection, eg the major 'col' classes: models, decks, dconf,
     * conf, tags are replaced with updated variants.
     *
     * UNSTABLE: DO NOT USE THIS ON A COLLECTION YOU CARE ABOUT.
     */
    @JvmStatic
    var defaultLegacySchema: Boolean = true

    /**
     * The language(es) the backend uses for translations.
     */
    var defaultLanguages: Iterable<String> = listOf("en")

    @JvmStatic
    private var backendForTesting: CustomBackendCreator? = null

    @JvmStatic
    @JvmOverloads
    fun getBackend(context: Context, languages: Iterable<String>? = null, legacySchema: Boolean? = null): Backend {
        val langs = languages ?: defaultLanguages
        val legacy = legacySchema ?: defaultLegacySchema
        return backendForTesting?.invoke(context, langs, legacy) ?: Backend(
                context,
                langs,
                legacy
        )
    }

    @JvmStatic
    fun setDefaultLanguagesFromLocales(locales: Iterable<Locale>) {
        defaultLanguages = locales.map { localeToBackendCode(it) }
    }

    private fun localeToBackendCode(locale: Locale): String {
        // TODO: this needs checking that all language codes match the ones
        // shown here: https://i18n.ankiweb.net/teams/
        return when (locale.language) {
            Locale("heb").language -> "he"
            Locale("yue").language -> "zh-TW"
            Locale("ind").language -> "id"
            Locale("tgl").language -> "tl"
            else -> locale.language
        }
    }

    /** Allows overriding the returned backend for unit tests */
    fun setOverride(creator: CustomBackendCreator?) {
        backendForTesting = creator
    }
}
