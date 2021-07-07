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

package net.ankiweb.rsdroid;

import androidx.sqlite.db.SupportSQLiteOpenHelper;

public abstract class BackendFactory {

    private BackendV1 backend;

    // Force users to go through getInstance - for now we need to handle the backend failure
    protected BackendFactory() {

    }

    @RustCleanup("Use BackendV[11/Next]Factory")
    public static BackendFactory createInstance() throws RustBackendFailedException {
        return BackendV11Factory.createInstance();
    }

    public synchronized BackendV1 getBackend() {
        if (backend == null) {
            backend = new BackendMutex(new BackendV1Impl());
        }
        return backend;
    }

    public synchronized void closeCollection() {
        if (backend == null) {
            return;
        }

        // we could swallow the exception here, most of the time it will be "collection is already closed"
        backend.closeCollection(false);
    }
    
    public abstract SupportSQLiteOpenHelper.Factory getSQLiteOpener();
}
