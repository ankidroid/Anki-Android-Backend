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

import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabaseLockedException;
import android.database.sqlite.SQLiteException;

import net.ankiweb.rsdroid.database.testutils.DatabaseComparison;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;

@RunWith(Parameterized.class)
public class DatabaseExceptionTest extends DatabaseComparison {

    @Test
    public void testBadSqlException() {
        try {
            mDatabase.execSQL("select 2 from b");
        } catch (SQLiteException ex) {
            // Original: no such table: b (code 1): , while compiling: select 1 from b
            // Rust: error while compiling: select 2 from b: DBError { info: "SqliteFailure(Error { code: Unknown, extended_code: 1 }, Some(\"no such table: b\"))", kind: Other }
            String message = ex.getLocalizedMessage();
            assertThat(message, containsString("no such table: b"));
            assertThat(message, containsString("1"));
            assertThat(message, containsString("select 2 from b"));
        }
    }

    @Test
    public void testConstraintViolation() {
        try {
            mDatabase.execSQL("CREATE TABLE test (id int PRIMARY KEY)");
            mDatabase.execSQL("INSERT INTO test (id) VALUES (1)");
            mDatabase.execSQL("INSERT INTO test (id) VALUES (1)");
            assertThat("Exception should be thrown", true, is(false));
        } catch (SQLiteConstraintException ex) {
            // Java: "column id is not unique (code 19)"
            // Rust: UNIQUE constraint failed: test.id
            // fully: DBError { info: "SqliteFailure(Error { code: ConstraintViolation, extended_code: 1555 }, Some(\"UNIQUE constraint failed: test.id\"))", kind: Other }

            assertThat(ex.getMessage(), containsString("id"));
            assertThat(ex.getMessage(), anyOf(containsString("unique"), containsString("UNIQUE")));
        }
    }

    @Test
    @Ignore("TODO: Not yet handled")
    public void testDatabaseLocked() {
        // required for check database

        // For this test, we need to lock in rsdroid before opening the collection,
        // which is quite a lot of effort due to JNI being harder to write


        // This is broken in-app, but only slightly on an error case.
        // Using the in-app lock, this causes a hang rather than corruption.
        // I suspect the outcome will be different if performed out-of-process - maybe an exception?

        try {
            mDatabase.execSQL("PRAGMA locking_mode = EXCLUSIVE; BEGIN EXCLUSIVE;");
        } catch (SQLiteDatabaseLockedException ex) {
            // assert
        }
    }
}
