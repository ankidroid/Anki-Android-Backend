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

package net.ankiweb;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.protobuf.InvalidProtocolBufferException;

import net.ankiweb.rsdroid.BackendException;
import net.ankiweb.rsdroid.BackendV1;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import BackendProto.Backend;
import BackendProto.Backend.OpenCollectionIn;
import BackendProto.Backend.SchedTimingTodayOut;

@RunWith(AndroidJUnit4.class)
public class BackendIntegrationTests extends InstrumentedTest {

    @Test
    public void testBackendException() {
        BackendV1 backendV1 = new BackendV1();
        try {
            SchedTimingTodayOut ret = backendV1.schedTimingToday();
            Assert.fail("call should have failed - needs an open collection");
        } catch (BackendException ex) {
            // OK
        }
    }

    @Test
    public void schedTimingTodayCall() throws BackendException {
        BackendV1 backendV1 = new BackendV1();
        String path = getAssetFilePath("initial_version_2_12_1.anki2");
        backendV1.openCollection(OpenCollectionIn.newBuilder().setCollectionPath(path).build());
        SchedTimingTodayOut ret = backendV1.schedTimingToday();

        int elpased = ret.getDaysElapsed();
        long nextDayAt = ret.getNextDayAt();
    }
}