package net.ankiweb.rsdroid;

import androidx.annotation.CheckResult;
import androidx.annotation.Nullable;

import com.google.protobuf.InvalidProtocolBufferException;

import net.ankiweb.rsdroid.database.SQLHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import BackendProto.Backend;

public class BackendV1 extends net.ankiweb.rsdroid.BackendService implements SQLHandler {

    private Pointer mBackEndPointer = null;

    @Override
    @CheckResult
    public Pointer ensureBackend() {
        if (mBackEndPointer == null) {
            Backend.BackendInit.Builder builder = Backend.BackendInit.newBuilder()
                    .setServer(false)
                    .addPreferredLangs("en")
                    .setLocaleFolderPath("");
            long backendPointer = NativeMethods.openBackend(builder.build().toByteArray());

            mBackEndPointer = new Pointer(backendPointer);
        }
        return mBackEndPointer;
    }

    public void openAnkiDroidCollection(String path) throws BackendException {
        openAnkiDroidCollection(Backend.OpenCollectionIn.newBuilder().setCollectionPath(path).build());
    }


    public void openAnkiDroidCollection(Backend.OpenCollectionIn args) throws BackendException {
        try {
            Pointer backendPointer = ensureBackend();
            byte[] result = NativeMethods.openCollection(backendPointer.toJni(), args.toByteArray());
            Backend.Empty message = Backend.Empty.parseFrom(result);
            validateMessage(result, message);
        } catch (InvalidProtocolBufferException ex) {
            throw new BackendException(ex);
        }
    }

    // the main openCollection does an upgrade to V15, which is not ideal
    @Override
    public void openCollection(@Nullable String collectionPath, @Nullable String mediaFolderPath, @Nullable String mediaDbPath, @Nullable String logPath) throws BackendException {
        Backend.OpenCollectionIn in = Backend.OpenCollectionIn.newBuilder()
                .setCollectionPath(collectionPath)
                .setMediaFolderPath(mediaFolderPath)
                .setMediaDbPath(mediaDbPath)
                .setLogPath(logPath)
                .build();
        openAnkiDroidCollection(in);
    }

    @SuppressWarnings("CharsetObjectCanBeUsed")
    @CheckResult
    public JSONArray fullQuery(String sql, @Nullable Object... args) {
        try {
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

        byte[] data = o.toString().getBytes(Charset.forName("UTF-8"));

        Pointer backend = ensureBackend();
        byte[] result = NativeMethods.fullDatabaseCommand(backend.toJni(), data);

        String json = new String(result);

        try {
            return new JSONArray(json);
        } catch (Exception e) {
            // TODO: We have a protobuf-error
            throw new RuntimeException(json, e);
        }
    }

    public long insertForId(String sql, Object[] args) throws BackendException {
        try {
            List<Object> asList = args == null ? new ArrayList<>() : Arrays.asList(args);
            JSONObject o = new JSONObject();
            o.put("sql", sql);
            o.put("args", new JSONArray(asList));

            byte[] data = o.toString().getBytes(Charset.forName("UTF-8"));

            Pointer backend = ensureBackend();
            byte[] result = NativeMethods.sqlInsertForId(backend.toJni(), data);

            Backend.Int64 message = Backend.Int64.parseFrom(result);
            validateMessage(result, message);
            return message.getVal();

        } catch (JSONException e) {
            throw new RuntimeException(e);
        } catch (InvalidProtocolBufferException e) {
            throw new BackendException(e);
        }
    }

    public int executeGetRowsAffected(String sql, Object[] bindArgs) throws BackendException {
        try {
            List<Object> asList = bindArgs == null ? new ArrayList<>() : Arrays.asList(bindArgs);
            JSONObject o = new JSONObject();
            o.put("sql", sql);
            o.put("args", new JSONArray(asList));

            byte[] data = o.toString().getBytes(Charset.forName("UTF-8"));

            Pointer backend = ensureBackend();
            byte[] result = NativeMethods.sqlQueryForAffected(backend.toJni(), data);

            Backend.Int32 message = Backend.Int32.parseFrom(result);
            validateMessage(result, message);
            return message.getVal();
        } catch (JSONException e) {
            throw new RuntimeException(e);
        } catch (InvalidProtocolBufferException e) {
            throw new BackendException(e);
        }
    }

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
            JSONObject o = new JSONObject();

            o.put("kind", kind);

            byte[] data = o.toString().getBytes(Charset.forName("UTF-8"));

            Pointer backend = ensureBackend();
            byte[] result = NativeMethods.fullDatabaseCommand(backend.toJni(), data);

            String asString = new String(result);

            if (!"null".equals(asString)) {
                // TODO: Handle as protobuf error
                throw new RuntimeException(asString);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public String[] getColumnNames(String sql) {
        return NativeMethods.getColumnNames(ensureBackend().toJni(), sql);
    }
}
