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
package net.ankiweb.rsdroid.database

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import net.ankiweb.rsdroid.BackendFactory
import net.ankiweb.rsdroid.BackendUtils.openAnkiDroidCollection
import net.ankiweb.rsdroid.Backend
import net.ankiweb.rsdroid.BackendUtils
import timber.log.Timber

class RustV11SupportSQLiteOpenHelper : RustSupportSQLiteOpenHelper {
    constructor(configuration: SupportSQLiteOpenHelper.Configuration, backendFactory: BackendFactory?) : super(configuration, backendFactory) {}
    constructor(backend: Backend) : super(backend) {}

    override fun createRustSupportSQLiteDatabase(readOnly: Boolean): SupportSQLiteDatabase? {
        Timber.d("createRustSupportSQLiteDatabase")
        return if (configuration != null) {
            val backend = backendFactory!!.getBackend()
            openAnkiDroidCollection(backend, configuration.name, true)
            RustSupportSQLiteDatabase(backend, readOnly)
        } else {
            RustSupportSQLiteDatabase(backend, readOnly)
        }
    }
}