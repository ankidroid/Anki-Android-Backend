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

package net.ankiweb.rsdroid.database.testutils;

import androidx.annotation.NonNull;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory;

import net.ankiweb.rsdroid.BackendFactory;
import net.ankiweb.rsdroid.BackendUtils;
import net.ankiweb.rsdroid.InstrumentedTest;
import net.ankiweb.rsdroid.database.RustSQLiteOpenHelperFactory;

import org.junit.Before;
import org.junit.runners.Parameterized;

import java.util.Arrays;

public class DatabaseComparison extends InstrumentedTest {

    @Parameterized.Parameter
    public DatabaseType schedVersion;
    protected SupportSQLiteDatabase mDatabase;

    @Parameterized.Parameters(name = "{0}")
    public static java.util.Collection<Object[]> initParameters() {
        // This does one run with schedVersion injected as 1, and one run as 2
        return Arrays.asList(new Object[][] { { DatabaseType.FRAMEWORK }, { DatabaseType.RUST } });
    }

    @Before
    public void setUp() {
        try {
            mDatabase = getDatabase();
            mDatabase.execSQL("create table nums (id int)");
        } catch (Exception e) {
            if (!handleSetupException(e)) {
                throw e;
            }
        }
    }

    protected boolean handleSetupException(Exception e) {
        return false;
    }

    protected SupportSQLiteDatabase getDatabase() {
        SupportSQLiteOpenHelper.Configuration config = SupportSQLiteOpenHelper.Configuration.builder(getContext())
                .callback(new DefaultCallback())
                .name(getDatabasePath())
                .build();

        switch (schedVersion) {
            case RUST:
                BackendFactory mBackendFactory = new BackendFactory();
                // This throws on corruption
                // This doesn't
                BackendUtils.openAnkiDroidCollection(mBackendFactory.getInstance(), getDatabasePath());
                return new RustSQLiteOpenHelperFactory(mBackendFactory).create(config).getWritableDatabase();
            case FRAMEWORK:
                return new FrameworkSQLiteOpenHelperFactory().create(config).getWritableDatabase();
        }
        throw new IllegalStateException();
    }

    protected String getDatabasePath() {
        // TODO: look into this - null should work
        switch (schedVersion) {
            case RUST: return ":memory:";
            case FRAMEWORK: return null;
            default: return null;
        }
    }

    public enum DatabaseType {
        FRAMEWORK,
        RUST
    }



    private static class DefaultCallback extends SupportSQLiteOpenHelper.Callback {
        public DefaultCallback() {
            super(1);
        }

        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {

        }

        @Override
        public void onUpgrade(@NonNull SupportSQLiteDatabase db, int oldVersion, int newVersion) {

        }

        @Override
        public void onCorruption(@NonNull SupportSQLiteDatabase db) {
            // do nothing
        }
    }
}
