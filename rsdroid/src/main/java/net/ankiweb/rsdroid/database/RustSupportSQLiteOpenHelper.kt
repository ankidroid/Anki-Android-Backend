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
import net.ankiweb.rsdroid.Backend

abstract class RustSupportSQLiteOpenHelper : SupportSQLiteOpenHelper {
    protected val configuration: SupportSQLiteOpenHelper.Configuration?
    protected val backend: Backend?
    protected var backendFactory: BackendFactory? = null
    protected var database: SupportSQLiteDatabase? = null

    constructor(configuration: SupportSQLiteOpenHelper.Configuration, backendFactory: BackendFactory?) {
        this.configuration = configuration
        this.backendFactory = backendFactory
        backend = null
    }

    constructor(backend: Backend) {
        check(backend.isOpen()) { "Backend should be open" }
        this.backend = backend
        configuration = null
    }

    override fun getDatabaseName(): String? {
        return backend?.getPath()
                ?: if (configuration != null) {
                    configuration.name
                } else {
                    throw IllegalStateException("Class invalid: no config or backend")
                }
    }

    override fun setWriteAheadLoggingEnabled(enabled: Boolean) {
        throw NotImplementedException()
    }

    override fun getWritableDatabase(): SupportSQLiteDatabase {
        if (database == null) {
            database = createRustSupportSQLiteDatabase(false)
        }
        return database!!
    }

    override fun getReadableDatabase(): SupportSQLiteDatabase {
        throw NotImplementedException("Not supported by Rust - requires open collection")
    }

    override fun close() {}
    protected abstract fun createRustSupportSQLiteDatabase(readOnly: Boolean): SupportSQLiteDatabase?
}