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

package net.ankiweb.rsdroid.ankiutil;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.test.platform.app.InstrumentationRegistry;

import net.ankiweb.rsdroid.BackendFactory;
import net.ankiweb.rsdroid.BackendUtils;
import net.ankiweb.rsdroid.BackendV1;
import net.ankiweb.rsdroid.RustBackendFailedException;

import org.junit.Before;

public class InstrumentedTest {

    static {
        Log.e("InstrumentedTest", "Timber has been disabled.");
    }

    @Before
    public void before() {
        /*
        Timber added 1 minute to the stress test (1m18 -> 2m30). Didn't seem worth it.
        Timber.uprootAll();
        Timber.plant(new Timber.DebugTree());
        */
    }

    protected String getAssetFilePath(String fileName) {
        try {
            return Shared.getTestFilePath(getContext(), fileName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

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

    protected Context getContext() {
        return InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    protected BackendV1 getBackend(String fileName) {
        BackendV1 backendV1 = getClosedBackend();
        String path = getAssetFilePath(fileName);
        BackendUtils.openAnkiDroidCollection(backendV1, path);
        return backendV1;
    }

    protected BackendV1 getClosedBackend() {
        try {
            return BackendFactory.createInstance().getBackend();
        } catch (RustBackendFailedException e) {
            throw new RuntimeException(e);
        }
    }
}
