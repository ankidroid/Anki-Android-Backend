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

import net.ankiweb.rsdroid.BackendV1
import net.ankiweb.rsdroid.BackendV1Impl
import androidx.sqlite.db.SupportSQLiteOpenHelper
import net.ankiweb.rsdroid.RustCleanup
import kotlin.Throws
import net.ankiweb.rsdroid.RustBackendFailedException
import net.ankiweb.rsdroid.BackendFactory
import net.ankiweb.rsdroid.BackendV11Factory

abstract class BackendFactory  // Force users to go through getInstance - for now we need to handle the backend failure
protected constructor() {
    private var backend: BackendV1? = null
    @Synchronized
    fun getBackend(): BackendV1 {
        if (backend == null) {
            backend = BackendV1Impl() // new BackendMutex(new BackendV1Impl());
        }
        return backend!!
    }

    @Synchronized
    fun closeCollection() {
        if (backend == null) {
            return
        }

        // we could swallow the exception here, most of the time it will be "collection is already closed"
//        backend.closeCollection(false);
    }

    abstract val sQLiteOpener: SupportSQLiteOpenHelper.Factory?

    companion object {
        @JvmStatic
        @RustCleanup("Use BackendV[11/Next]Factory")
        @Throws(RustBackendFailedException::class)
        open fun createInstance(): BackendFactory {
            return BackendV11Factory.createInstance()
        }
    }
}