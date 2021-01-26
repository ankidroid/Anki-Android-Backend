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

import androidx.sqlite.db.SupportSQLiteDatabase;

import net.ankiweb.rsdroid.BackendV1;
import net.ankiweb.rsdroid.ankiutil.InstrumentedTest;

import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

import timber.log.Timber;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;

public class StreamingProtobufSQLiteCursorTest extends InstrumentedTest {

    @Test
    public void testPaging() throws IOException {

        try (BackendV1 backend = super.getBackend("initial_version_2_12_1.anki2")) {
            SupportSQLiteDatabase db = new RustSupportSQLiteOpenHelper(backend).getWritableDatabase();

            db.execSQL("create table tmp (id int)");

            for (int i = 0; i < 999; i++) {
                db.execSQL("insert into tmp VALUES (?)", new Object[] { i });
            }

            iterateAllRows(db); // 999

            db.execSQL("insert into tmp VALUES (?)", new Object[] { 999 });

            iterateAllRows(db); // 1000

            db.execSQL("insert into tmp VALUES (?)", new Object[] { 1000 });

            iterateAllRows(db); // 1001

        }
    }

    private void iterateAllRows(SupportSQLiteDatabase db) {
        long position = 0;

        try (Cursor cur = db.query("SELECT * from tmp")) {
            while (cur.moveToNext()) {
                assertThat(cur.getLong(0), is(position));
                position++;
            }
        }
    }

    @Test
    public void testCorruptionIsHandled() throws IOException {
        int elements = StreamingProtobufSQLiteCursor.RUST_PAGE_SIZE;

        try (BackendV1 backend = super.getBackend("initial_version_2_12_1.anki2")) {
            SupportSQLiteDatabase db = new RustSupportSQLiteOpenHelper(backend).getWritableDatabase();

            db.execSQL("create table tmp (id int)");
            for (int i = 0; i < elements + 1; i++) {
                db.execSQL("insert into tmp (id) values (?)", new Object[] { i });
            }

            try (Cursor c1 = db.query("select * from tmp order by id asc")) {

                for (int i = 0; i < elements; i++) {
                    Timber.d("start %d", i);
                    c1.moveToNext();
                    assertThat(c1.getInt(0), is(i));
                    Timber.d("end %d", i);
                }

                try (Cursor c2 = db.query("select id + 5 from tmp order by id asc")) {
                    for (int i = 0; i < elements; i++) {
                        c2.moveToNext();
                        assertThat(c2.getInt(0), is(i + 5));
                    }

                    c1.moveToNext();

                    // This should fail as we've overwritten the cache.
                    assertThat(c1.getInt(0), is(elements));
                }
            }
        }

    }
}
