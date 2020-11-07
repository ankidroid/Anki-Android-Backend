package net.ankiweb.rsdroid;

import android.annotation.SuppressLint;
import android.os.Build;

import androidx.annotation.CheckResult;
import androidx.annotation.Nullable;

public class NativeMethods {

    private static boolean hasSetUp = false;
    private static RustBackendFailedException setupException;

    public static boolean isRoboUnitTest() {
        return "robolectric".equals(Build.FINGERPRINT);
    }


    public static synchronized void ensureSetup() throws RustBackendFailedException {
        if (hasSetUp) {
            if (setupException != null) {
                throw setupException;
            }
            return;
        }
        try {
            System.loadLibrary("rsdroid");
        } catch (UnsatisfiedLinkError e) {
            if (!isRoboUnitTest()) {
                setupException = new RustBackendFailedException(e);
                throw setupException;
            }
            // In Robolectric, assume setup works (setupException == null) if the library throws.
            // As the library is loaded at a later time (or a failure will be quickly found).
        }
        hasSetUp = true;
    }

    @CheckResult
    static native byte[] command(long backendPointer, final int command, byte[] args);
    @CheckResult
    static native long openBackend(byte[] data);

    @SuppressLint("CheckResult")
    @SuppressWarnings("unused")
    static void execCommand(long backendPointer, final int command, byte[] args) {
        command(backendPointer, command, args);
    }

    @CheckResult
    static native byte[] openCollection(long backendPointer, byte[] data);


    /** Temporary: perform a database command and obtain the result as a JSON string without streaming. */
    @CheckResult
    static native byte[] fullDatabaseCommand(long backendPointer, byte[] data);

    /**
     * Performs an insert and returns the last inserted row id.
     * data: json encoded data
     */
    @CheckResult
    static native byte[] sqlInsertForId(long backendPointer, byte[] data);

    @CheckResult
    static native byte[] sqlQueryForAffected(long backendPointer, byte[] data);

    @Nullable
    @CheckResult
    static native String[] getColumnNames(long backendPointer, String sql);
}
