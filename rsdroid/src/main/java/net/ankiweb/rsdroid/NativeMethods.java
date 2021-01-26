package net.ankiweb.rsdroid;

import android.annotation.SuppressLint;
import android.os.Build;

import androidx.annotation.CheckResult;
import androidx.annotation.Nullable;

import timber.log.Timber;

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
        } finally {
            hasSetUp = true;
        }
    }

    public static byte[] executeCommand(long backendPointer, final int command, byte[] args) {
        Timber.i("ExecuteCommand: %s", net.ankiweb.rsdroid.RustBackendMethods.commandName(command));
        return command(backendPointer, command, args);
    }

    @CheckResult
    private static native byte[] command(long backendPointer, final int command, byte[] args);

    @CheckResult
    static native long openBackend(byte[] data);

    @SuppressLint("CheckResult")
    static void execCommand(long backendPointer, final int command, byte[] args) {
        command(backendPointer, command, args);
    }

    @CheckResult
    static native byte[] openCollection(long backendPointer, byte[] data);


    /** Temporary: perform a database command and obtain the result as a JSON string without streaming. */
    @CheckResult
    static native byte[] fullDatabaseCommand(long backendPointer, byte[] data);

    /** Input: JSON serialized request
     * @return DbResult object */
    @CheckResult
    static native byte[] databaseCommand(long backendPointer, byte[] data);

    /** Returns the next page of results after a databaseCommand.
     * @return DbResult object */
    @CheckResult
    static native byte[] databaseGetNextResultPage(long backendPointer, int sequenceNumber, int page);
    
    /** Clears the memory from the current protobuf query. */
    static native int cancelCurrentProtoQuery(long backendPointer, int sequenceNumber);

    /** Clears the memory from the all protobuf queries. */
    static native void cancelAllProtoQueries(long backendPointer);

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

    static native long closeBackend(long backendPointer);

    static native byte[] executeAnkiDroidCommand(long backendPointer, int command, byte[] args);
}
