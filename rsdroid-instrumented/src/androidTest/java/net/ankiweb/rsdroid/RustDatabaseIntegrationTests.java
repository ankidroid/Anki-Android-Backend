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

import android.content.ContentValues;
import android.database.Cursor;

import androidx.annotation.CheckResult;

import net.ankiweb.rsdroid.ankiutil.InstrumentedTest;
import net.ankiweb.rsdroid.ankiutil.RustDatabaseUtil;
import net.ankiweb.rsdroid.database.RustSupportSQLiteDatabase;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

public class RustDatabaseIntegrationTests extends InstrumentedTest {

    private static String fileName = "initial_version_2_12_1.anki2";

    @Test
    public void testScalar() {
        RustSupportSQLiteDatabase database = getDatabase();

        int returnValue = RustDatabaseUtil.queryScalar(database, "select 2");

        assertThat(returnValue, is(2));
    }

    @Test
    public void testArgs() {
        RustSupportSQLiteDatabase database = getDatabase();

        int returnValue = RustDatabaseUtil.queryScalar(database, "select ?", 1);

        assertThat(returnValue, is(1));
    }

    @Test
    public void testNullArgs() {
        // We really shouldn't pass in null here, but we do.
        RustSupportSQLiteDatabase database = getDatabase();

        int returnValue = RustDatabaseUtil.queryScalar(database, "select 3", (Object[]) null);

        assertThat(returnValue, is(3));
    }

    @Test
    public void testNullArgValue() {
        RustSupportSQLiteDatabase database = getDatabase();

        int returnValue = RustDatabaseUtil.queryScalar(database, "select 4", (Object) null);

        assertThat(returnValue, is(3));
    }

    @Test
    public void testNullIsCastToZero() {
        // now, we get into the nitty-gritty of SQLite
        RustSupportSQLiteDatabase database = getDatabase();

        int returnValue = RustDatabaseUtil.queryScalar(database, "select null");

        assertThat(returnValue, is(0));
    }

    @Test
    public void testNullFloatIsCastToZero() {
        // now, we get into the nitty-gritty of SQLite
        RustSupportSQLiteDatabase database = getDatabase();

        int returnValue = RustDatabaseUtil.queryScalar(database, "select null");

        assertThat(returnValue, is(0));
    }

    @Test
    public void testMultipleParameters() {
        RustSupportSQLiteDatabase database = getDatabase();

        Cursor returnValue = database.query("select ?, ?", new Object[] { 1, 2 });

        returnValue.moveToFirst();
        assertThat(returnValue.getInt(0), is(1));
        assertThat(returnValue.getInt(1), is(2));
    }

    @Test
    public void testInsert() {
        RustSupportSQLiteDatabase database = getDatabase();

        database.query("Create table test (id int)");

        long ret = database.insertForForId("insert into test (id) values (3)", null);

        // first row inserted is 1.
        assertThat(ret, is(1L));


        long ret2 = database.insertForForId("insert into test (id) values (2)", null);
        assertThat(ret2, is(2L));
    }

    @Test
    public void testUpdate() {
        RustSupportSQLiteDatabase database = getDatabase();

        database.query("Create table test (id int)");

        database.insertForForId("insert into test (id) values (3)", null);
        database.insertForForId("insert into test (id) values (4)", null);
        database.insertForForId("insert into test (id) values (5)", null);


        ContentValues values = new ContentValues();
        values.put("id", 2);
        int ret2 = database.update("test", 0, values, "id <> 4", null);
        assertThat(ret2, is(2));

        int result = RustDatabaseUtil.queryScalar(database, "select count(*) from test where id = 2");
        assertThat(result, is(2));
    }

    @Test
    public void testMoveToFirst() {
        RustSupportSQLiteDatabase database = getDatabase();

        String query = "select count(), sum(time)/1000 from revlog where id > 100";

        Cursor cur = null;
        try {
            cur = database.query(query, null);

            cur.moveToFirst();
            int count = cur.getInt(0);
            int minutes = (int) Math.round(cur.getInt(1) / 60.0);
            assertThat(count, is(0));
            assertThat("null is converted to 0 implicitly", minutes, is(0));
        } finally {
            if (cur != null && !cur.isClosed()) {
                cur.close();
            }
        }
    }

    @Test
    public void testTransactionSuccess() {
        RustSupportSQLiteDatabase db =  getDatabase();

        db.execSQL("create table test (id int)");

        db.beginTransaction();
        try {
            assertThat("In transaction", db.inTransaction(), is(true));
            db.execSQL("insert into test (id) values (1)");
            db.setTransactionSuccessful();
        } finally {
            if (db.inTransaction()) {
                db.endTransaction();
            } else {
                fail("not in transaction");
            }
        }

        assertFalse("transaction should have ended", db.inTransaction());
        assertThat(RustDatabaseUtil.queryScalar(db, "select count(*) from test"), is(1));
    }

    @Test
    public void testTransactionFailure() {
        RustSupportSQLiteDatabase db =  getDatabase();

        db.execSQL("create table test (id int)");

        db.beginTransaction();
        try {
            db.execSQL("insert into test (id) values (1)");
            db.execSQL("insert into invalid (abc) values (5)");
            fail();
        } catch (Exception e) {
            // expected
        } finally {
            if (db.inTransaction()) {
                db.endTransaction();
            } else {
                fail("not in transaction");
            }
        }

        assertFalse("transaction should be ended", db.inTransaction());
        assertThat(RustDatabaseUtil.queryScalar(db, "select count(*) from test"), is(0));
    }


    @CheckResult
    private RustSupportSQLiteDatabase getDatabase() {
        try {
            BackendV1 backendV1 = getBackend(fileName);
            boolean readOnly = false;
            return new RustSupportSQLiteDatabase(backendV1, readOnly);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
