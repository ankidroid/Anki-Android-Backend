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

import androidx.annotation.Nullable;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;

import net.ankiweb.rsdroid.BackendFactory;

public class RustSupportSQLiteOpenHelper implements SupportSQLiteOpenHelper {
    private final Configuration mConfiguration;
    private BackendFactory mBackendFactory;

    public RustSupportSQLiteOpenHelper(Configuration configuration, BackendFactory backendFactory) {
        this.mConfiguration = configuration;
        this.mBackendFactory = backendFactory;
    }

    @Nullable
    @Override
    public String getDatabaseName() {
        return mConfiguration.name;
    }

    @Override
    public void setWriteAheadLoggingEnabled(boolean enabled) {
        throw new NotImplementedException();
    }

    // TODO:
    @Override
    public SupportSQLiteDatabase getWritableDatabase() {
        return new RustSupportSQLiteDatabase(mBackendFactory.getBackend(), mConfiguration.name);
    }

    @Override
    public SupportSQLiteDatabase getReadableDatabase() {
        return new RustSupportSQLiteDatabase(mBackendFactory.getBackend(), mConfiguration.name);
    }

    @Override
    public void close() {

    }
}
