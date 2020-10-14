package net.ankiweb.rsdroid;

import android.annotation.SuppressLint;
import android.os.Build;

import androidx.annotation.CheckResult;
import androidx.annotation.Nullable;

public class NativeMethods {

    public static boolean isRoboUnitTest() {
        return "robolectric".equals(Build.FINGERPRINT);
    }

    static {
        try {
            System.loadLibrary("rsdroid");
        } catch (UnsatisfiedLinkError e) {
            if (!isRoboUnitTest()) {
                throw e;
            }
        }

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
    //UnsatisfiedLinkError
}
