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

import androidx.annotation.NonNull;
import androidx.sqlite.db.SupportSQLiteDatabase;

import net.ankiweb.rsdroid.ankiutil.DatabaseUtil;
import net.ankiweb.rsdroid.database.testutils.DatabaseComparison;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isOneOf;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
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
        testStringConversion(SQLOutput.asInt("1"), "1");
        testStringConversion(SQLOutput.asFloat("1.6"), "1.6");
        testStringConversion(SQLOutput.asText("hi"), "hi");
        testStringConversion(SQLOutput.asNull(), null);
    }

    @Test
    public void testFailingStringConversions() {
        try {
            testStringConversion(SQLOutput.asBlob(), "unused");
        } catch (SQLiteException e) {
           assertThat(e.getMessage(), isOneOf("unknown error (code 0): Unable to convert BLOB to string",
                   "unknown error (code 0 SQLITE_OK): Unable to convert BLOB to string"));
        }
    }

    @Test
    public void testIntConversions() {
        testIntConversion(SQLOutput.asInt("1"), 1);
        testIntConversion(SQLOutput.asFloat("1.6"), 1);
        testIntConversion(SQLOutput.asNull(), 0);
        testIntConversion(SQLOutput.asText("hi"), 0);
        testIntConversion(SQLOutput.asText("2"), 2);
        testIntConversion(SQLOutput.asText("2.52"), 2);
    }

    @Test
    public void testFailingIntConversions() {
        try {
            testIntConversion(SQLOutput.asBlob(), 42);
        } catch (SQLiteException e) {
            assertThat(e.getMessage(), isOneOf("unknown error (code 0): Unable to convert BLOB to long",
                    "unknown error (code 0 SQLITE_OK): Unable to convert BLOB to long"));
        }
    }

    @Test
    public void testFloatConversions() {
        testFloatConversion(SQLOutput.asInt("1"), 1.0f);
        testFloatConversion(SQLOutput.asFloat("1.6"), 1.6f);
        testFloatConversion(SQLOutput.asNull(), 0.0f);
        testFloatConversion(SQLOutput.asText("hi"), 0.0f);
        testFloatConversion(SQLOutput.asText("2"), 2f);
        testFloatConversion(SQLOutput.asText("2.52"), 2.52f);
    }

    @Test
    public void testFailingFloatConversions() {
        try {
            testFloatConversion(SQLOutput.asBlob(), 42);
        } catch (SQLiteException e) {
            assertThat(e.getMessage(), isOneOf("unknown error (code 0): Unable to convert BLOB to double",
                    "unknown error (code 0 SQLITE_OK): Unable to convert BLOB to double"));
        }
    }

    @Test
    public void testDoubleConversions() {
        testDoubleConversion(SQLOutput.asInt("1"), 1.0d);
        testDoubleConversion(SQLOutput.asFloat("1.6"), 1.6d);
        testDoubleConversion(SQLOutput.asNull(), 0.0d);
        testDoubleConversion(SQLOutput.asText("hi"), 0.0d);
        testDoubleConversion(SQLOutput.asText("2"), 2d);
        testDoubleConversion(SQLOutput.asText("2.52"), 2.52d);
    }

    @Test
    public void testFailingDoubleConversions() {
        try {
            testDoubleConversion(SQLOutput.asBlob(), 42);
        } catch (SQLiteException e) {
            assertThat(e.getMessage(), isOneOf("unknown error (code 0): Unable to convert BLOB to double",
                    "unknown error (code 0 SQLITE_OK): Unable to convert BLOB to double"));
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

    @Test
    public void testBackwards() {
        SupportSQLiteDatabase db = mDatabase;

        db.execSQL("create table test (id int)");

        for (int i = 0; i < DB_PAGE_NUM_INT_ELEMENTS + 1; i++) {
            db.execSQL("insert into test VALUES (1)");
        }

        // tests to see if moveToPrevious() returns true or false if position == 0
        try (Cursor c = db.query("select * from test")) {
            c.moveToLast();
            while (c.moveToPrevious()) {
                c.getLong(0);
            }
            assertThat(c.getPosition(), is(-1));
        }
    }

    @Test
    public void moveToBeforeFirst() {
        SupportSQLiteDatabase db = mDatabase;

        db.execSQL("create table test (id int)");

        for (int i = 0; i < DB_PAGE_NUM_INT_ELEMENTS + 1; i++) {
            db.execSQL("insert into test VALUES (1)");
        }

        try (Cursor c = db.query("select * from test")) {
            assertThat(c.getPosition(), is(-1));
            assertTrue(c.moveToFirst());
            assertThat(c.getPosition(), is(0));
            // despite returning false, it works
            assertFalse("moveToPosition(-1) should return false, but should work", c.moveToPosition(-1));
            assertThat(c.getPosition(), is(-1));
        }
    }

    @Test
    public void testRealConversionIssue() {

        SupportSQLiteDatabase db = mDatabase;

        db.execSQL("create table if not exists revlog (" + "   id              integer primary key,"
                + "   cid             integer not null," + "   usn             integer not null,"
                + "   ease            integer not null," + "   ivl             integer not null,"
                + "   lastIvl         integer not null," + "   factor          integer not null,"
                + "   time            integer not null," + "   type            integer not null)");


        // one in ms: 1622631861000 (Wednesday, 2 June 2021 11:04:21)
        // two in ms: 1622691861001 ( Thursday, 3 June 2021 03:44:21.001)

        db.execSQL("insert into revlog (id, cid, usn, ease, ivl, lastIvl, factor, time, type) VALUES (1, 1, 0, 1, 10, 5, 250, 1622631861000, 1)");
        db.execSQL("insert into revlog (id, cid, usn, ease, ivl, lastIvl, factor, time, type) VALUES (2, 1, 0, 1, 10, 5, 250, 1622691861001, 1)");

        ArrayList<double[]> list = new ArrayList<>(7); // one by day of the week
        String query = "SELECT strftime('%w',datetime( cast(id/ 1000  -" + 3600 +
                " as int), 'unixepoch')) as wd, " +
                "sum(case when ease = 1 then 0 else 1 end) / " +
                "cast(count() as float) * 100, " +
                "count() " +
                "from revlog " +
                "group by wd " +
                "order by wd";

        try (Cursor cur = db.query(query)) {
            while (cur.moveToNext()) {
                list.add(new double[] { cur.getDouble(0), cur.getDouble(1), cur.getDouble(2) });
            }
        }

        ArrayList<double[]> expected = new ArrayList<>();
        expected.add(new double[] { 3.0, 0.0, 2.0});
        assertThat(list.get(0), is(expected.get(0)));
    }


    public void testStringConversion(SQLOutput output, String expected) {
        testConversion(output, c -> c.getString(0), expected);
    }

    public void testIntConversion(SQLOutput output, int expected) {
        testConversion(output, c -> c.getInt(0), expected);
    }

    public void testFloatConversion(SQLOutput output, float expected) {
        testConversion(output, c -> c.getFloat(0), expected);
    }

    public void testDoubleConversion(SQLOutput output, double expected) {
        testConversion(output, c -> c.getDouble(0), expected);
    }

    public void testConversion(SQLOutput output, Function<Cursor, Object> f, Object expected) {
        try (Cursor c = mDatabase.query("select cast(" + output.value + " as " + output.getSqlType() + " )")) {
            c.moveToFirst();
            assertThat(f.apply(c), is(expected));
        }
    }

    private enum SQLiteType {
        NULL,
        INTEGER,
        FLOAT,
        STRING,
        BLOB,
        ;

        public String getSqlType() {
            switch (this) {
                case INTEGER: return "INTEGER";
                case FLOAT: return "REAL";
                case NULL:
                case STRING:
                    return "TEXT";
                case BLOB: return "BLOB";
                default: throw new IllegalStateException("Unexpected type");
            }
        }
    }

    public static class SQLOutput {
        public SQLiteType type;
        public String value;

        public static SQLOutput asText(@NonNull String value) {
            return new SQLOutput(SQLiteType.STRING, "\"" + value + "\"");
        }

        public static SQLOutput asFloat(@NonNull String value) {
            return new SQLOutput(SQLiteType.FLOAT,  value );
        }

        public static SQLOutput asInt(@NonNull String value) {
            return new SQLOutput(SQLiteType.INTEGER, value);
        }

        public static SQLOutput asNull() {
            return new SQLOutput(SQLiteType.NULL, null);
        }

        public static SQLOutput asBlob() {
            return new SQLOutput(SQLiteType.BLOB, "\"aa\""); // Unsure about this
        }

        public SQLOutput(SQLiteType type, String value) {
            this.type = type;
            this.value = value;
        }


        public String getSqlType() {
            return type.getSqlType();
        }
    }

    /** Required until we implement desugaring */
    private interface Function<T, R> {
        R apply(T t);
    }
}
