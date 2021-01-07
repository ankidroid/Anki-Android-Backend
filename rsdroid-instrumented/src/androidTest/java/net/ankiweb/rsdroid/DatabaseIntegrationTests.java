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
    public void testNullFloatIsCastToZero() {
        // now, we get into the nitty-gritty of SQLite

        // TODO: This is not correct - not a float
        int returnValue = DatabaseUtil.queryScalar(mDatabase, "select null");

        assertThat(returnValue, is(0));
    }

    @Test
    public void testMultipleParameters() {
        Cursor returnValue = mDatabase.query("select ?, ?", new Object[] { 1, 2 });

        returnValue.moveToFirst();
        assertThat(returnValue.getInt(0), is(1));
        assertThat(returnValue.getInt(1), is(2));
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
}
