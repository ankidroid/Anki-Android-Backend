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

import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.sqlite.SQLiteException;

import androidx.sqlite.db.SupportSQLiteDatabase;

import net.ankiweb.rsdroid.ankiutil.DatabaseUtil;
import net.ankiweb.rsdroid.database.testutils.DatabaseComparison;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

@RunWith(Parameterized.class)
public class DatabaseIntegrationTests extends DatabaseComparison {

    private static final int INT_SIZE_BYTES = 8;
    private static final int OPTIONAL_BYTES = 1;
    /** Number of integers in 1 page of DB results when under test (111) */
    public static int DB_PAGE_NUM_INT_ELEMENTS = TEST_PAGE_SIZE / (INT_SIZE_BYTES + OPTIONAL_BYTES);

    @Test
    public void testScalar() {
        int returnValue = DatabaseUtil.queryScalar(mDatabase, "select 2");

        assertThat(returnValue, is(2));
    }

    @Test
    public void testArgs() {
        int returnValue = DatabaseUtil.queryScalar(mDatabase, "select ?", 1);

        assertThat(returnValue, is(1));
    }

    @Test
    public void testNullArgs() {
        // We really shouldn't pass in null here, but we do.
        int returnValue = DatabaseUtil.queryScalar(mDatabase, "select 3", (Object[]) null);

        assertThat(returnValue, is(3));
    }

    @Test
    public void testNullArgValue() {
        try {
            DatabaseUtil.queryScalar(mDatabase, "select 4", (Object) null);
        } catch (IllegalArgumentException ex) {
            assertThat(ex.getMessage(), is("Cannot bind argument at index 1 because the index is out of range.  The statement has 0 parameters."));
        }
    }

    @Test
    public void testNullIsCastToZero() {
        // now, we get into the nitty-gritty of SQLite

        int returnValue = DatabaseUtil.queryScalar(mDatabase, "select null");

        assertThat(returnValue, is(0));
    }

    @Test
    public void testNullDoubleIsCastToZero() {
        // now, we get into the nitty-gritty of SQLite

        double returnValue = DatabaseUtil.queryScalarFloat(mDatabase, "select null");

        assertThat(returnValue, is(0d));
    }

    @Test
    public void testMultipleParameters() {
        try (Cursor returnValue = mDatabase.query("select ?, ?", new Object[] { 1, 2 })) {
            returnValue.moveToFirst();
            assertThat(returnValue.getInt(0), is(1));
            assertThat(returnValue.getInt(1), is(2));
        }
    }

    @Test
    public void testTransactionSuccess() {
        mDatabase.execSQL("create table test (id int)");

        mDatabase.beginTransaction();
        try {
            assertThat("In transaction", mDatabase.inTransaction(), is(true));
            mDatabase.execSQL("insert into test (id) values (1)");
            mDatabase.setTransactionSuccessful();
        } finally {
            if (mDatabase.inTransaction()) {
                mDatabase.endTransaction();
            } else {
                fail("not in transaction");
            }
        }

        assertFalse("transaction should have ended", mDatabase.inTransaction());
        assertThat(DatabaseUtil.queryScalar(mDatabase, "select count(*) from test"), is(1));
    }

    @Test
    public void testTransactionFailure() {
        mDatabase.execSQL("create table test (id int)");

        mDatabase.beginTransaction();
        try {
            mDatabase.execSQL("insert into test (id) values (1)");
            mDatabase.execSQL("insert into invalid (abc) values (5)");
            fail();
        } catch (Exception e) {
            // expected
        } finally {
            if (mDatabase.inTransaction()) {
                mDatabase.endTransaction();
            } else {
                fail("not in transaction");
            }
        }

        assertFalse("transaction should be ended", mDatabase.inTransaction());
        assertThat(DatabaseUtil.queryScalar(mDatabase, "select count(*) from test"), is(0));
    }

    @Test
    public void testCursorIndexException() {
        mDatabase.execSQL("create table tmp (id int)");

        mDatabase.execSQL("insert into tmp (id) VALUES (?)", new Object[] { 1 } );

        try (Cursor c = mDatabase.query("select * from tmp")) {
            try {
                c.getString(0);
                fail("no exception thrown");
            } catch (CursorIndexOutOfBoundsException e) {
                assertThat(e.getMessage(), is("Index -1 requested, with a size of 1"));
            }
        }
    }

    @Test
    public void testStringConversions() {
        testStringConversion("int", "1");
        testStringConversion("double", "1.6");
        testStringConversion("string", "hi");
        testStringConversion("null", null);
    }

    @Test
    public void testFailingStringConversions() {
        try {
            testStringConversion("byte", "?");
        } catch (SQLiteException e) {
           assertThat(e.getMessage(), is("unknown error (code 0): Unable to convert BLOB to string"));
        }
    }

    @Test
    public void testIntConversions() {
        testIntConversion("int", 1);
        testIntConversion("double", 1); // this is a .floor
        testIntConversion("string", 0);
        testIntConversion("null", 0);
    }

    @Test
    public void testFailingIntConversions() {
        try {
            testIntConversion("byte", 42);
        } catch (SQLiteException e) {
            assertThat(e.getMessage(), is("unknown error (code 0): Unable to convert BLOB to long"));
        }
    }

    @Test
    public void testFloatConversions() {
        testFloatConversion("int", 1.0f);
        testFloatConversion("double", 1.6f);
        testFloatConversion("string", 0.0f); // yes - really?
        testFloatConversion("null", 0.0f);
    }

    @Test
    public void testFailingFloatConversions() {
        try {
            testFloatConversion("byte", 42);
        } catch (SQLiteException e) {
            assertThat(e.getMessage(), is("unknown error (code 0): Unable to convert BLOB to double"));
        }
    }

    @Test
    public void testDoubleConversions() {
        testDoubleConversion("int", 1.0d);
        testDoubleConversion("double", 1.6d);
        testDoubleConversion("string", 0.0d); // yes - really?
        testDoubleConversion("null", 0);
    }

    @Test
    public void testFailingDoubleConversions() {
        try {
            testDoubleConversion("byte", 42);
        } catch (SQLiteException e) {
            assertThat(e.getMessage(), is("unknown error (code 0): Unable to convert BLOB to double"));
        }
    }

    @Test
    public void testRowCountEmpty() {
        SupportSQLiteDatabase db = mDatabase;

        db.execSQL("create table test (id int)");

        try (Cursor c = db.query("select * from test")) {
            assertThat(c.getCount(), is(0));
        }
    }

    @Test
    public void testRowCountSingle() {
        SupportSQLiteDatabase db = mDatabase;

        db.execSQL("create table test (id int)");
        db.execSQL("insert into test VALUES (1)");

        try (Cursor c = db.query("select * from test")) {
            assertThat(c.getCount(), is(1));
        }
    }

    @Test
    public void testRowCountPage() {
        SupportSQLiteDatabase db = mDatabase;

        db.execSQL("create table test (id int)");

        for (int i = 0; i < DB_PAGE_NUM_INT_ELEMENTS; i++) {
            db.execSQL("insert into test VALUES (1)");
        }

        try (Cursor c = db.query("select * from test")) {
            assertThat(c.getCount(), is(DB_PAGE_NUM_INT_ELEMENTS));
        }
    }

    @Test
    public void testRowCountPageAndOne() {
        SupportSQLiteDatabase db = mDatabase;

        db.execSQL("create table test (id int)");

        for (int i = 0; i < DB_PAGE_NUM_INT_ELEMENTS + 1; i++) {
            db.execSQL("insert into test VALUES (1)");
        }

        try (Cursor c = db.query("select * from test")) {
            assertThat(c.getCount(), is(DB_PAGE_NUM_INT_ELEMENTS + 1));
        }
    }

    public void testStringConversion(String type, String expected) {
        testConversion(c -> c.getString(0), type, expected);
    }

    public void testIntConversion(String type, int expected) {
        testConversion(c -> c.getInt(0), type, expected);
    }

    public void testDoubleConversion(String type, double expected) {
        testConversion(c -> c.getDouble(0), type, expected);
    }

    public void testFloatConversion(String type, float expected) {
        testConversion(c -> c.getFloat(0), type, expected);
    }

    // Note: We don't test null or blob - blob is unused, didn't feel worth testing null

    public void testConversion(Function<Cursor, Object> f, String type, Object expected) {
        mDatabase.execSQL("DROP TABLE IF EXISTS tmp");

        String sqlType = type.equals("null") ? "string" : type;
        mDatabase.execSQL(String.format("create table tmp (val %s)", sqlType));

        Object result;
        switch (type) {
            case "int" : result = 1; break;
            case "double" : result = 1.6d; break; // we select 1.6 as the op is a .floor
            case "string" : result = "hi"; break;
            case "byte" : result = new byte[] { 1, 3, 3, 7 }; break;
            case "null": result = null; break;
            default: throw new IllegalStateException("test fail: unknown type: " + type);
        }

        mDatabase.execSQL("insert into tmp (val) VALUES (?)", new Object[] { result } );

        try (Cursor c = mDatabase.query("select * from tmp")) {
            c.moveToFirst();
            assertThat(f.apply(c), is(expected));
        }
    }


    private interface Function<T, R> {
        R apply(T t);
    }
}
