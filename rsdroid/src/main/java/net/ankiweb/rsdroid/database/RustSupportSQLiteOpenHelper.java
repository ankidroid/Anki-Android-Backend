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

import timber.log.Timber;

public class RustSupportSQLiteOpenHelper implements SupportSQLiteOpenHelper {
    @Nullable
    private final Configuration configuration;
    @Nullable
    private final BackendV1 backend;
    private BackendFactory backendFactory;
    private RustSupportSQLiteDatabase database;

    public RustSupportSQLiteOpenHelper(@NonNull Configuration configuration, BackendFactory backendFactory) {
        this.configuration = configuration;
        this.backendFactory = backendFactory;
        this.backend = null;
    }

    public RustSupportSQLiteOpenHelper(@NonNull BackendV1 backend) {
        if (!backend.isOpen()) {
            throw new IllegalStateException("Backend should be open");
        }
        this.backend = backend;
        configuration = null;
    }

    @Nullable
    @Override
    public String getDatabaseName() {
        if (backend != null) {
            return backend.getPath();
        } else if (configuration != null) {
            return configuration.name;
        } else {
            throw new IllegalStateException("Class invalid: no config or backend");
        }
    }

    @Override
    public void setWriteAheadLoggingEnabled(boolean enabled) {
        throw new NotImplementedException();
    }

    @Override
    public SupportSQLiteDatabase getWritableDatabase() {
        if (database == null) {
            this.database = createRustSupportSQLiteDatabase(false);
        }
        return database;
    }

    @Override
    public SupportSQLiteDatabase getReadableDatabase() {
        throw new NotImplementedException("Not supported by Rust - requires open collection");
    }

    @Override
    public void close() {

    }

    private RustSupportSQLiteDatabase createRustSupportSQLiteDatabase(@SuppressWarnings("SameParameterValue") boolean readOnly) {
        Timber.d("createRustSupportSQLiteDatabase");
        if (configuration != null) {
            BackendV1 backend = backendFactory.getBackend();
            BackendUtils.openAnkiDroidCollection(backend, configuration.name);
            return new RustSupportSQLiteDatabase(backend, readOnly);
        } else {
            return new RustSupportSQLiteDatabase(backend, readOnly);
        }
    }
}
