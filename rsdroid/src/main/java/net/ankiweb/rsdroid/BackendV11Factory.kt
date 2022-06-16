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

import androidx.sqlite.db.SupportSQLiteOpenHelper
import net.ankiweb.rsdroid.NativeMethods.ensureSetup
import net.ankiweb.rsdroid.RustBackendFailedException
import net.ankiweb.rsdroid.database.RustV11SQLiteOpenHelperFactory

class BackendV11Factory : BackendFactory() {
    override val sQLiteOpener: SupportSQLiteOpenHelper.Factory
        get() = RustV11SQLiteOpenHelperFactory(this)

    companion object {
        /**
         * Obtains an instance of BackendFactory which will connect to rsdroid.
         * Each call will generate a separate instance which can handle a new Anki collection
         */
        @RustV1Cleanup("RustBackendFailedException may be moved to a more appropriate location")
        @Throws(RustBackendFailedException::class)
        fun createInstance(): BackendV11Factory {
            ensureSetup()
            return BackendV11Factory()
        }
    }
}