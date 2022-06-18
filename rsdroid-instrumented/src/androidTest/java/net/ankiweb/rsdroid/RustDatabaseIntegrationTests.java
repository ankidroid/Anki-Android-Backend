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

import net.ankiweb.rsdroid.ankiutil.DatabaseUtil;
import net.ankiweb.rsdroid.ankiutil.InstrumentedTest;
import net.ankiweb.rsdroid.database.RustSupportSQLiteDatabase;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class RustDatabaseIntegrationTests extends InstrumentedTest {

    public static final String fileName = "initial_version_2_12_1.anki2";

    @Test
    public void testMoveToFirst() {
        String query = "select count(), sum(time)/1000 from revlog where id > 100";

        Cursor cur = null;
        try {
            cur = getDatabase().query(query, null);

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

        int result = DatabaseUtil.queryScalar(database, "select count(*) from test where id = 2");
        assertThat(result, is(2));
    }


    @CheckResult
    private RustSupportSQLiteDatabase getDatabase() {
        try {
            Backend backendV1 = getBackend(fileName);
            boolean readOnly = false;
            return new RustSupportSQLiteDatabase(backendV1);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
