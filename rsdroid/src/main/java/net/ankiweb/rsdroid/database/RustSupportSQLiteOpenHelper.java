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

package net.ankiweb.rsdroid.database;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;

import net.ankiweb.rsdroid.BackendFactory;
import net.ankiweb.rsdroid.BackendUtils;
import net.ankiweb.rsdroid.BackendV1;

public class RustSupportSQLiteOpenHelper implements SupportSQLiteOpenHelper {
    @Nullable
    private final Configuration mConfiguration;
    @Nullable
    private final BackendV1 mBackend;
    private BackendFactory mBackendFactory;
    private RustSupportSQLiteDatabase mDatabase;

    public RustSupportSQLiteOpenHelper(@NonNull Configuration configuration, BackendFactory backendFactory) {
        this.mConfiguration = configuration;
        this.mBackendFactory = backendFactory;
        mBackend = null;
    }

    public RustSupportSQLiteOpenHelper(@NonNull BackendV1 backend) {
        if (!backend.isOpen()) {
            throw new IllegalStateException("Backend should be open");
        }
        this.mBackend = backend;
        mConfiguration = null;
    }

    @Nullable
    @Override
    public String getDatabaseName() {
        if (mBackend != null) {
            return mBackend.getPath();
        } else {
            return mConfiguration.name;
        }
    }

    @Override
    public void setWriteAheadLoggingEnabled(boolean enabled) {
        throw new NotImplementedException();
    }

    @Override
    public SupportSQLiteDatabase getWritableDatabase() {
        if (mDatabase == null) {
            this.mDatabase = createRustSupportSQLiteDatabase(false);
        }
        return mDatabase;
    }

    @Override
    public SupportSQLiteDatabase getReadableDatabase() {
        throw new NotImplementedException("Not supported by Rust - requires open collection");
    }

    @Override
    public void close() {

    }

    private RustSupportSQLiteDatabase createRustSupportSQLiteDatabase(boolean readOnly) {
        if (mConfiguration != null) {
            BackendV1 backend = mBackendFactory.getBackend();
            BackendUtils.openAnkiDroidCollection(backend, mConfiguration.name);
            return new RustSupportSQLiteDatabase(backend, readOnly);
        } else {
            return new RustSupportSQLiteDatabase(mBackend, readOnly);
        }
    }
}
