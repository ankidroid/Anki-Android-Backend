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

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.CheckResult;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertTrue;

public class Shared {

    /**
     * Utility method to write to a file.
     * Throws the exception, so we can report it in syncing log
     */
    private static void writeToFileImpl(InputStream source, String destination) throws IOException {
        File f = new File(destination);
        try {
            f.createNewFile();

            @SuppressLint("DirectSystemCurrentTimeMillisUsage")
            long startTimeMillis = System.currentTimeMillis();
            long sizeBytes = CompatHelper.getCompat().copyFile(source, destination);
            @SuppressLint("DirectSystemCurrentTimeMillisUsage")
            long endTimeMillis = System.currentTimeMillis();

            long durationSeconds = (endTimeMillis - startTimeMillis) / 1000;
            long sizeKb = sizeBytes / 1024;
            long speedKbSec = 0;
            if (endTimeMillis != startTimeMillis) {
                speedKbSec = sizeKb * 1000 / (endTimeMillis - startTimeMillis);
            }
        } catch (IOException e) {
            throw new IOException(f.getName() + ": " + e.getLocalizedMessage(), e);
        }
    }

    /**
     * Calls {@link #writeToFileImpl(InputStream, String)} and handles IOExceptions
     * Does not close the provided stream
     * @throws IOException Rethrows exception after a set number of retries
     */
    public static void writeToFile(InputStream source, String destination) throws IOException {
        // sometimes this fails and works on retries (hardware issue?)
        final int retries = 5;
        int retryCnt = 0;
        boolean success = false;
        while (!success && retryCnt++ < retries) {
            try {
                writeToFileImpl(source, destination);
                success = true;
            } catch (IOException e) {
                if (retryCnt == retries) {
                    throw e;
                } else {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * @param name An additional suffix to ensure the test directory is only used by a particular resource.
     * @return See getTestDir.
     */
    private static File getTestDir(Context context, String name) {
        String suffix = "";
        if (!TextUtils.isEmpty(name)) {
            suffix = "-" + name;
        }
        File dir = new File(context.getCacheDir(), "testfiles" + suffix);
        if (!dir.exists()) {
            assertTrue(dir.mkdir());
        }
        File[] files = dir.listFiles();
        if (files == null) {
            // Had this problem on an API 16 emulator after a stress test - directory existed
            // but listFiles() returned null due to EMFILE (Too many open files)
            // Don't throw here - later file accesses will provide a better exception.
            // and the directory exists, even if it's unusable.
            return dir;
        }

        for (File f : files) {
            assertTrue(f.delete());
        }
        return dir;
    }

    /**
     * Copy a file from the application's assets directory and return the absolute path of that
     * copy.
     *
     * Files located inside the application's assets collection are not stored on the file
     * system and can not return a usable path, so copying them to disk is a requirement.
     */
    @CheckResult
    public static String getTestFilePath(Context context, String name) throws IOException {
        InputStream is = context.getClassLoader().getResourceAsStream("assets/" + name);
        if (is == null) {
            throw new FileNotFoundException("Could not find test file: assets/" + name);
        }
        String dst = new File(getTestDir(context, name), name).getAbsolutePath();
        writeToFile(is, dst);
        return dst;
    }
}
