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

package net.ankiweb.rsdroid.testing;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;

import org.apache.commons.exec.OS;

public class ModuleLoader {

    private static boolean sLoaded;
    private static final HashMap<String, String> sCache = new HashMap<>();

    public static void init() {
        if (!sLoaded) {
            if (OS.isFamilyWindows()) {
                load("rsdroid", ".dll");
            } else if (OS.isFamilyMac()) {
                load("librsdroid", ".dylib");
            } else if (OS.isFamilyUnix()) {
                load("librsdroid", ".so");
            } else {
                String osName = System.getProperty("os.name");
                throw new IllegalStateException(String.format("Could not determine OS Type for: '%s'", osName));
            }

            sLoaded = true;
        }
    }

    private static void load(String filename, String extension) {
        String path;
        try {
            path = getPathFromResourceStream(filename, extension);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            Runtime.getRuntime().load(path);
        } catch (UnsatisfiedLinkError e) {
            if (e.getMessage() == null || !e.getMessage().contains("already loaded in another classloader")) {
                throw e;
            }
        }
    }

    private static String getPathFromResourceStream(String fileName, String extension) throws IOException {
        // TODO: Ensure that this is reasonably handled without too much copying.
        String fullFilename = fileName + extension;

        if (sCache.containsKey(fullFilename)) {
            return sCache.get(fullFilename);
        }

        // Note: this will leave some data in the temp folder.
        String path = File.createTempFile(fileName, extension).getAbsolutePath();
        File targetFile = new File(path);
        if (targetFile.exists() && targetFile.length() > 0) {
            return path;
        }

        InputStream rsdroid = ModuleLoader.class.getClassLoader().getResourceAsStream(fullFilename);
        if (rsdroid == null) {
            throw new IllegalStateException("Could not find " + fullFilename);
        }


        OutputStream outStream;
        try {
            outStream = new FileOutputStream(targetFile);
        } catch (Exception e) {
            throw new IOException("Could not open output file", e);
        }

        byte[] buffer = new byte[8 * 1024];
        int bytesRead;
        while ((bytesRead = rsdroid.read(buffer)) != -1) {
            outStream.write(buffer, 0, bytesRead);
        }

        rsdroid.close();
        outStream.close();

        sCache.put(fullFilename, path);

        return path;
    }
}