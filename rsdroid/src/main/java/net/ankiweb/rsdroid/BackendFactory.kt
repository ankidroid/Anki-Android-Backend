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

abstract class BackendFactory  // Force users to go through getInstance - for now we need to handle the backend failure
protected constructor() {
    private var backend: Backend? = null

    @Synchronized
    fun getBackend(): Backend {
        if (backend == null) {
            backend = Backend()
        }
        return backend!!
    }

    abstract val sQLiteOpener: SupportSQLiteOpenHelper.Factory?

    companion object {
        @JvmStatic
        @RustCleanup("Use BackendV[11/Next]Factory")
        @Throws(RustBackendFailedException::class)
        fun createInstance(): BackendFactory {
            return BackendV11Factory.createInstance()
        }
    }
}