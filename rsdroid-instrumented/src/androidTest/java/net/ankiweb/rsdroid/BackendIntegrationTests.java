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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

import BackendProto.Backend.SchedTimingTodayOut;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

@RunWith(AndroidJUnit4.class)
public class BackendIntegrationTests extends InstrumentedTest {

    /** Ensure that the database can't be locked */
    @Rule
    public Timeout timeout = new Timeout(3, TimeUnit.SECONDS);

    @Before
    public void test() {
        if (!isEmulator()) {
            throw new IllegalArgumentException("do not run on real device yet");
        }
    }

    @Test
    public void testBackendException() {
        BackendV1 backendV1 = getClosedBackend();
        try {
            SchedTimingTodayOut ret = backendV1.schedTimingToday();
            Assert.fail("call should have failed - needs an open collection");
        } catch (BackendException ex) {
            // OK
        }
    }

    @Test
    public void schedTimingTodayCall() {
        BackendV1 backendV1 = getBackend("initial_version_2_12_1.anki2");
        SchedTimingTodayOut ret = backendV1.schedTimingToday();
        int elapsed = ret.getDaysElapsed();
        long nextDayAt = ret.getNextDayAt();
    }
    @Test
    public void collectionIsVersion11AfterOpen() {
        // This test will be decomissioned, but before we get an upgrade stategy, we need to ensure we're not upgrading the database.

        BackendV1 backendV1 = getBackend("initial_version_2_12_1.anki2");
        backendV1.closeCollection(false);

        assertThat("Needs assertion", false, is(true));
    }

    @Test
    public void fullQueryTest() {
        BackendV1 backendV1 = getBackend("initial_version_2_12_1.anki2");
        JSONArray result = backendV1.fullQuery("select * from col");
    }

    @Test
    public void columnNamesTest() {
        BackendV1 backendV1 = getBackend("initial_version_2_12_1.anki2");
        String[] names = backendV1.getColumnNames("select * from col");

        assertThat(names, is(new String[] { "id", "crt", "mod", "scm", "ver", "dty", "usn", "ls", "conf", "models", "decks", "dconf", "tags" }));
    }
}