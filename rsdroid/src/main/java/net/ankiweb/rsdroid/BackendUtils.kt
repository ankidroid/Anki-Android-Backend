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

object BackendUtils {
    /**
     *
     * @throws android.database.sqlite.SQLiteDatabaseCorruptException If database is corrupt
     */
    // fixme: call backend directly
    @JvmStatic
    fun openAnkiDroidCollection(backendV1: Backend, path: String?, forceSchema11: Boolean) {
        backendV1.openCollection(path ?: ":memory:", forceSchema11 = forceSchema11)
    }

    val ankiCommitHash: String
        get() = BuildConfig.ANKI_COMMIT_HASH
}