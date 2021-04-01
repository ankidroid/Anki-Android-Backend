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

import androidx.annotation.CheckResult;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.google.protobuf.InvalidProtocolBufferException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Closeable;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import BackendProto.AdBackend;
import BackendProto.Backend;
import BackendProto.Sqlite;
import timber.log.Timber;

/**
 * Do not use an instance of this class directly - it should be handled via BackendMutex
 * All public methods should be accessed via interface
 * */
public class BackendV1Impl extends net.ankiweb.rsdroid.RustBackendImpl implements BackendV1, Closeable {

    /*
    Note: java.lang.RuntimeException: com.google.protobuf.InvalidProtocolBufferException: Protocol message end-group tag did not match expected tag.
    Implies that ?
     */

    private Pointer backEndPointer = null;
    @Nullable
    private String collectionPath;
    private boolean isDisposed;
    private final AnkiDroidBackendImpl ankiDroidBackend;

    // intentionally package private - use BackendFactory
    BackendV1Impl() {
        ankiDroidBackend = new AnkiDroidBackendImpl(this::ensureBackend);
    }

    /**
     * Obtains a pointer to the Rust backend.
     * This pointer is to a structure containing environment-level data such as the
     * optional collection reference,
     *
     * Java is responsible for disposing of this pointer, or a memory leak will occur.
     *
     * TODO: This defines the locales, logging and translation files. We do not use these yet.
     * */
    @Override
    @CheckResult
    public Pointer ensureBackend() {
        if (isDisposed) {
            throw new BackendException("Backend has been closed");
        }

        if (backEndPointer == null) {
            boolean isServer = false;
            String langs = "en";
            String localeFolderPath = "";

            Timber.i("Opening rust backend. Server: %b. Langs: '%s', path: %s", isServer, langs, localeFolderPath);

            Backend.BackendInit.Builder builder = Backend.BackendInit.newBuilder()
                    .setServer(isServer)
                    .addPreferredLangs(langs)
                    .setLocaleFolderPath(localeFolderPath);

            long backendPointer = NativeMethods.openBackend(builder.build().toByteArray());

            backEndPointer = new Pointer(backendPointer);
        }
        return backEndPointer;
    }

    @Override
    protected byte[] executeCommand(long backendPointer, int command, byte[] args) {
        return NativeMethods.executeCommand(backendPointer, command, args);
    }

    @Override
    public boolean isOpen() {
        return backEndPointer != null;
    }

    @Override
    public void close() {
        Timber.i("Closing rust backend");
        if (backEndPointer != null) {
            try {
                closeDatabase();
            } catch (BackendException ex) {
                // Typically: CollectionNotOpen
                Timber.w(ex, "Error while closing rust database");
            }
            Timber.d("Executing close backend command");
            NativeMethods.closeBackend(backEndPointer.toJni());
        }
        this.isDisposed = true;
        backEndPointer = null;
    }

    public void openAnkiDroidCollection(Backend.OpenCollectionIn args) {
        collectionPath = args.getCollectionPath();
        try {
            Pointer backendPointer = ensureBackend();
            Timber.i("Opening Collection: '%s' '%s' '%s' '%s'", args.getCollectionPath(), args.getLogPath(), args.getMediaDbPath(), args.getMediaFolderPath());
            byte[] result = NativeMethods.openCollection(backendPointer.toJni(), args.toByteArray());
            Backend.Empty message = Backend.Empty.parseFrom(result);
            validateMessage(result, message);
        } catch (BackendException.BackendDbException ex) {
            collectionPath = null;
            throw ex.toSQLiteException("openAnkiDroidCollection");
        } catch (InvalidProtocolBufferException ex) {
            collectionPath = null;
            throw BackendException.fromException(ex);
        }
    }

    // the main openCollection does an upgrade to V15, which is not ideal
    @Override
    public void openCollection(@Nullable String collectionPath, @Nullable String mediaFolderPath, @Nullable String mediaDbPath, @Nullable String logPath) {
        Backend.OpenCollectionIn in = Backend.OpenCollectionIn.newBuilder()
                .setCollectionPath(collectionPath)
                .setMediaFolderPath(mediaFolderPath)
                .setMediaDbPath(mediaDbPath)
                .setLogPath(logPath)
                .build();
        openAnkiDroidCollection(in);
    }

    @CheckResult
    public JSONArray fullQuery(String sql, @Nullable Object... args) {
        try {
            Timber.i("Rust: SQL query: '%s'", sql);
            return fullQueryInternal(sql, args);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private JSONArray fullQueryInternal(String sql, @Nullable Object[] args) throws JSONException {
        List<Object> asList = args == null ? new ArrayList<>() : Arrays.asList(args);
        JSONObject o = new JSONObject();

        o.put("kind", "query");
        o.put("sql", sql);
        o.put("args", new JSONArray(asList));
        o.put("first_row_only", false);

        byte[] data = jsonToBytes(o);

        Pointer backend = ensureBackend();
        byte[] result = NativeMethods.fullDatabaseCommand(backend.toJni(), data);

        String json = new String(result);

        try {
            return new JSONArray(json);
        } catch (Exception e) {
            Backend.BackendError pbError;
            try {
                pbError = Backend.BackendError.parseFrom(result);
            } catch (InvalidProtocolBufferException invalidProtocolBufferException) {
                throw BackendException.fromException(invalidProtocolBufferException);
            }
            throw BackendException.fromError(pbError);
        }
    }

    public long insertForId(String sql, Object[] args) {
        try {
            Timber.i("Rust: sql insert %s", sql);

            List<Object> asList = args == null ? new ArrayList<>() : Arrays.asList(args);
            JSONObject o = new JSONObject();
            o.put("sql", sql);
            o.put("args", new JSONArray(asList));

            byte[] data = jsonToBytes(o);

            Pointer backend = ensureBackend();
            byte[] result = NativeMethods.sqlInsertForId(backend.toJni(), data);

            Backend.Int64 message = Backend.Int64.parseFrom(result);
            validateMessage(result, message);
            return message.getVal();

        } catch (JSONException e) {
            throw new RuntimeException(e);
        } catch (InvalidProtocolBufferException e) {
            throw BackendException.fromException(e);
        }
    }

    public int executeGetRowsAffected(String sql, Object[] bindArgs) {
        try {
            Timber.i("Rust: executeGetRowsAffected %s", sql);

            List<Object> asList = bindArgs == null ? new ArrayList<>() : Arrays.asList(bindArgs);
            JSONObject o = new JSONObject();
            o.put("sql", sql);
            o.put("args", new JSONArray(asList));

            byte[] data = jsonToBytes(o);

            Pointer backend = ensureBackend();
            byte[] result = NativeMethods.sqlQueryForAffected(backend.toJni(), data);

            Backend.Int32 message = Backend.Int32.parseFrom(result);
            validateMessage(result, message);
            return message.getVal();
        } catch (JSONException e) {
            throw new RuntimeException(e);
        } catch (InvalidProtocolBufferException e) {
            throw BackendException.fromException(e);
        }
    }

    /* Begin Protobuf-based database streaming methods (#6) */

    @Override
    public Sqlite.DBResponse fullQueryProto(String query, Object... args) {
        byte[] result = null;
        try {
            Timber.d("Rust: fullQueryProto %s", query);

            List<Object> asList = args == null ? new ArrayList<>() : Arrays.asList(args);
            JSONObject o = new JSONObject();

            o.put("kind", "query");
            o.put("sql", query);
            o.put("args", new JSONArray(asList));
            o.put("first_row_only", false);

            byte[] data = jsonToBytes(o);

            Pointer backend = ensureBackend();
            result = NativeMethods.databaseCommand(backend.toJni(), data);

            Sqlite.DBResponse message = Sqlite.DBResponse.parseFrom(result);
            validateMessage(result, message);
            return message;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        } catch (InvalidProtocolBufferException e) {
            validateResult(result);
            throw BackendException.fromException(e);
        }
    }

    @Override
    public Sqlite.DBResponse getNextSlice(long startIndex, int sequenceNumber) {
        byte[] result = null;
        try {
            Timber.d("Rust: getNextSlice %d", startIndex);

            Pointer backend = ensureBackend();
            result = NativeMethods.databaseGetNextResultPage(backend.toJni(), sequenceNumber, startIndex);

            Sqlite.DBResponse message = Sqlite.DBResponse.parseFrom(result);
            validateMessage(result, message);
            return message;
        } catch (InvalidProtocolBufferException e) {
            validateResult(result);
            throw BackendException.fromException(e);
        }
    }

    @Override
    public void cancelCurrentProtoQuery(int sequenceNumber) {
        Timber.d("cancelCurrentProtoQuery");
        NativeMethods.cancelCurrentProtoQuery(ensureBackend().toJni(), sequenceNumber);
    }

    @Override
    public void cancelAllProtoQueries() {
        Timber.d("cancelAllProtoQueries");
        NativeMethods.cancelAllProtoQueries(ensureBackend().toJni());

    }

    /* End protobuf-based database streaming methods */

    public void beginTransaction() {
        // Note: Casing is important here.
        performTransaction("begin");
    }


    public void commitTransaction() {
        performTransaction("commit");
    }

    public void rollbackTransaction() {
        performTransaction("rollback");
    }

    private void performTransaction(String kind) {
        try {
            Timber.i("Rust: transaction %s", kind);

            JSONObject o = new JSONObject();

            o.put("kind", kind);

            byte[] data = jsonToBytes(o);

            Pointer backend = ensureBackend();
            byte[] result = NativeMethods.fullDatabaseCommand(backend.toJni(), data);

            String asString = new String(result);

            if (!"null".equals(asString)) {
                Backend.BackendError ex = Backend.BackendError.parseFrom(result);
                throw BackendException.fromError(ex);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    public static void setPageSizeForTesting(long pageSizeInBytes) {
        // TODO: Make this nonstatic
        NativeMethods.setDbPageSize(pageSizeInBytes);
    }

    @Override
    public void setPageSize(long pageSizeInBytes) {
        NativeMethods.setDbPageSize(pageSizeInBytes);
    }

    @Override
    public String[] getColumnNames(String sql) {
        Timber.i("Rust: getColumnNames %s", sql);
        return NativeMethods.getColumnNames(ensureBackend().toJni(), sql);
    }

    @Override
    public void closeCollection(boolean downgradeToSchema11) {
        cancelAllProtoQueries();
        super.closeCollection(downgradeToSchema11);
    }

    @Override
    public void closeDatabase() {
        closeCollection(false);
    }

    @Override
    public String getPath() {
        return collectionPath;
    }

    @SuppressWarnings("CharsetObjectCanBeUsed")
    private byte[] jsonToBytes(JSONObject o) {
        return o.toString().getBytes(Charset.forName("UTF-8"));
    }

    @Override
    public AdBackend.SchedTimingTodayOut2 schedTimingTodayLegacy(long createdSecs, int createdMinsWest, long nowSecs, int nowMinsWest, int rolloverHour) {
        return ankiDroidBackend.schedTimingTodayLegacy(createdSecs, createdMinsWest, nowSecs, nowMinsWest, rolloverHour);
    }

    @Override
    public AdBackend.LocalMinutesWestOut localMinutesWestLegacy(long collectionCreationTime) {
        return ankiDroidBackend.localMinutesWestLegacy(collectionCreationTime);
    }

    @Override
    @RustCleanup("Architecture - backendPtr param is not required")
    public AdBackend.DebugActiveDatabaseSequenceNumbersOut debugActiveDatabaseSequenceNumbers(long backendPtr) {
        return ankiDroidBackend.debugActiveDatabaseSequenceNumbers(ensureBackend().toJni());
    }
}
