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

import android.os.Build;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import net.ankiweb.rsdroid.BackendException;
import net.ankiweb.rsdroid.BackendV1;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.concurrent.TimeUnit;

import BackendProto.Backend.SchedTimingTodayOut;

@RunWith(AndroidJUnit4.class)
public class BackendIntegrationTests extends InstrumentedTest {

    /** Ensure that the database can't be locked */
    @Rule
    public Timeout timeout = new Timeout(3, TimeUnit.SECONDS);

    /**
     * This is how google detects emulators in flutter and how react-native does it in the device info module
     * https://github.com/react-native-community/react-native-device-info/blob/bb505716ff50e5900214fcbcc6e6434198010d95/android/src/main/java/com/learnium/RNDeviceInfo/RNDeviceModule.java#L185
     * @return boolean true if the execution environment is most likely an emulator
     */
    protected static boolean isEmulator() {
        return (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.PRODUCT.contains("sdk_google")
                || Build.PRODUCT.contains("google_sdk")
                || Build.PRODUCT.contains("sdk")
                || Build.PRODUCT.contains("sdk_x86")
                || Build.PRODUCT.contains("vbox86p")
                || Build.PRODUCT.contains("emulator")
                || Build.PRODUCT.contains("simulator");
    }


    @Before
    public void test() {
        if (!isEmulator()) {
            throw new IllegalArgumentException("do not run on real device yet");
        }
    }

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
        backendV1.openAnkiDroidCollection(path);
        SchedTimingTodayOut ret = backendV1.schedTimingToday();
        int elapsed = ret.getDaysElapsed();
        long nextDayAt = ret.getNextDayAt();
    }

    @Test
    public void collectionIsVersion11AfterOpen() throws BackendException {
        // This test will be decomissioned, but before we get an upgrade stategy, we need to ensure we're not upgrading the database.

        BackendV1 backendV1 = new BackendV1();
        String path = getAssetFilePath("initial_version_2_12_1.anki2");
        backendV1.openAnkiDroidCollection(path);
        backendV1.closeCollection(false);
    }
}