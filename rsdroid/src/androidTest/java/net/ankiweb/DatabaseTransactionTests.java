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

package net.ankiweb;

import androidx.annotation.NonNull;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory;

import net.ankiweb.rsdroid.BackendFactory;
import net.ankiweb.rsdroid.database.NotImplementedException;
import net.ankiweb.rsdroid.database.RustSQLiteOpenHelperFactory;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@RunWith(Parameterized.class)
public class DatabaseTransactionTests extends InstrumentedTest {

    @Parameter
    public DatabaseType schedVersion;
    private SupportSQLiteDatabase mDatabase;

    @Parameters(name = "{0}")
    public static java.util.Collection<Object[]> initParameters() {
        // This does one run with schedVersion injected as 1, and one run as 2
        return Arrays.asList(new Object[][] { { DatabaseType.FRAMEWORK }, { DatabaseType.RUST } });
    }

    @Before
    public void setUp() {
        mDatabase = getDatabase();
        mDatabase.execSQL("create table nums (id int)");
    }

    @Test
    public void nestedTransactionFailureInside() {
        mDatabase.beginTransaction();
        mDatabase.beginTransaction();
        insert(1);
        // mDatabase.setTransactionSuccessful();
        mDatabase.endTransaction();
        insert(2);
        mDatabase.setTransactionSuccessful();
        mDatabase.endTransaction();

        assertThat(countNums(), is(0));
    }


    @Test
    public void nestedTransactionFailureOutside() {
        mDatabase.beginTransaction();
        mDatabase.beginTransaction();
        insert(1);
        mDatabase.setTransactionSuccessful();
        mDatabase.endTransaction();
        insert(2);
        // mDatabase.setTransactionSuccessful();
        mDatabase.endTransaction();

        assertThat("count should be rolled back", countNums(), is(0));
    }

    @Test
    public void nestedTransactionSuccess() {
        mDatabase.beginTransaction();
        mDatabase.beginTransaction();
        insert(1);
        mDatabase.setTransactionSuccessful();
        mDatabase.endTransaction();
        insert(2);
        mDatabase.setTransactionSuccessful();
        mDatabase.endTransaction();

        assertThat("count should be updated", countNums(), is(2));
    }

    private void insert(int rowNumber) {
        String s = String.format("insert into nums (id) values (%s)", rowNumber);
        mDatabase.execSQL(s);
    }

    private int countNums() {
        return RustDatabaseUtil.queryScalar(mDatabase, "select count(*) from nums");
    }

    private SupportSQLiteDatabase getDatabase() {
        SupportSQLiteOpenHelper.Configuration config = SupportSQLiteOpenHelper.Configuration.builder(getContext())
                .callback(new DefaultCallback())
                .name(null)
                .build();

        switch (schedVersion) {
            case RUST:
                BackendFactory mBackendFactory = new BackendFactory();
                // TODO: Does this work?
                try {
                    mBackendFactory.getInstance().openAnkiDroidCollection(":memory:");
                } catch (Exception e) {

                }
                return new RustSQLiteOpenHelperFactory(mBackendFactory).create(config).getWritableDatabase();
            case FRAMEWORK:
                return new FrameworkSQLiteOpenHelperFactory().create(config).getWritableDatabase();
        }
        throw new IllegalStateException();
    }


    enum DatabaseType {
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
    }
}
