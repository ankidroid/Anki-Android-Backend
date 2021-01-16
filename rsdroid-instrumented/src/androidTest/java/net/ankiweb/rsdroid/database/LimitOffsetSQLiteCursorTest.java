/*
 * Copyright (c) 2021 David Allison <davidallisongithub@gmail.com>
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

import android.database.Cursor;

import androidx.annotation.CheckResult;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import net.ankiweb.rsdroid.BackendV1;
import net.ankiweb.rsdroid.RustDatabaseIntegrationTests;
import net.ankiweb.rsdroid.ankiutil.InstrumentedTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static net.ankiweb.rsdroid.database.LimitOffsetSQLiteCursor.PAGE_SIZE;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@RunWith(AndroidJUnit4.class)
public class LimitOffsetSQLiteCursorTest extends InstrumentedTest {

    private SupportSQLiteDatabase mDatabase;

    @Before
    public void setup() {
        mDatabase = getDatabase();
    }

    @Test
    public void testEmpty() {
        testAddingRows(0);
    }

    @Test
    public void testOneRow() {
        testAddingRows(1);
    }


    @Test
    public void testFull() {
        testAddingRows(PAGE_SIZE);
    }


    @Test
    public void testNextPageOne() {
        testAddingRows(PAGE_SIZE + 1);
    }


    @Test
    public void testNextPageFull() {
        testAddingRows(PAGE_SIZE + PAGE_SIZE - 1);
    }


    @Test
    public void testTwoPages() {
        testAddingRows(PAGE_SIZE + PAGE_SIZE);
    }

    @Test
    public void changingPageSizeDoesNotAffectClass() {
        addRows(5);

        LimitOffsetSQLiteCursor c = (LimitOffsetSQLiteCursor) mDatabase.query("select * from tmp");
        assertThat(c.getPageSize(), is(100));

        PAGE_SIZE = 101;

        assertThat(c.getPageSize(), is(100));
        LimitOffsetSQLiteCursor c2 = (LimitOffsetSQLiteCursor) mDatabase.query("select * from tmp");
        assertThat(c2.getPageSize(), is(101));
    }

    private void testAddingRows(int rowCount) {
        addRows(rowCount);

        checkRows(rowCount);
    }

    private void checkRows(int expected) {

        try (Cursor c = mDatabase.query("select * from tmp")) {
            if (expected == 0) {
                assertThat(c.moveToFirst(), is(false));
            }

            for (int i = 0; i < expected - 1; i++) {
                assertThat("should have more rows", c.moveToNext(), is(true));
                assertThat(c.getString(0), is(Integer.toString(i)));
            }

        }
    }

    private void addRows(int rowCount) {
        mDatabase.query("create table tmp (id varchar)");

        for(int i = 0; i < rowCount; i++) {
            mDatabase.query("insert into tmp (id) VALUES (?)", new Object[] { Integer.toString(i) });
        }
    }

    @CheckResult
    private RustSupportSQLiteDatabase getDatabase() {
        try {
            BackendV1 backendV1 = getBackend(RustDatabaseIntegrationTests.fileName);
            boolean readOnly = false;
            return new RustSupportSQLiteDatabase(backendV1, readOnly);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
