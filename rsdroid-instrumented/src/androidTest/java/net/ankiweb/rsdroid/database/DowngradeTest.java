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

import net.ankiweb.rsdroid.BackendException;
import net.ankiweb.rsdroid.BackendV1;
import net.ankiweb.rsdroid.ankiutil.InstrumentedTest;

import org.json.JSONArray;
import org.json.JSONException;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class DowngradeTest extends InstrumentedTest {



    @Test
    public void downgradeWithLaterSchema() throws IOException, JSONException {
        String fileName = "schema_16.anki2";
        String path = getAssetFilePath(fileName);
        try (BackendV1 backendV1 = super.getBackendFromPath(path) ){
            assertSchemaVer(backendV1, 11);
        }
        fileName = "schema_17.anki2";
        path = getAssetFilePath(fileName);
        try (BackendV1 backendV1 = super.getBackendFromPath(path) ){
            assertSchemaVer(backendV1, 11);
        }
    }

    private void assertSchemaVer(BackendV1 backendV1, @SuppressWarnings("SameParameterValue") int expectedVersion) throws JSONException {
        JSONArray array = backendV1.fullQuery("select ver from col");

        assertThat(array.length(), is(1));
        JSONArray subArray = array.getJSONArray(0);
        assertThat(subArray.length(), is(1));
        assertThat(subArray.optInt(0, 0), is(expectedVersion));
    }

    @SuppressWarnings({"unused", "RedundantSuppression"})
    private void assertOpeningFails(String path) {
        try (BackendV1 unused = super.getBackendFromPath(path)) {
            fail();
        } catch (Exception e) {
            // ignore
        }
    }
}
