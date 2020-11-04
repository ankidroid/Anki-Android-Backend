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

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.json.JSONArray;
import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(AndroidJUnit4.class)
public class BackendMutexTest extends InstrumentedTest {

    @Test
    public void ensureDatabaseInTransactionIsLocked() throws BackendException, JSONException, InterruptedException {

        BackendMutex b = (BackendMutex) super.getBackend("initial_version_2_12_1.anki2");

        b.fullQuery("create table test (id int)");

        AtomicBoolean transactionBegun = new AtomicBoolean(false);
        new Thread(() -> {
            try {
                b.beginTransaction();
                transactionBegun.set(true);
                Thread.sleep(500);

                b.fullQuery("insert into test (id) values (1)");

            } catch (InterruptedException | BackendException e) {
                throw new RuntimeException(e);
            } finally {
                transactionBegun.set(true);
                b.commitTransaction();
            }
        }).start();

        // we risk calling the query before the thread has begin
        while (!transactionBegun.get()) {
            Thread.sleep(10);
        }

        JSONArray p = b.fullQuery("select count(*) from test");

        assertThat(p.getJSONArray(0).getInt(0), is(1));
    }
}
