package net.ankiweb.rsdroid;

import androidx.annotation.Nullable;

import com.google.protobuf.InvalidProtocolBufferException;

import BackendProto.Backend;

public class BackendV1 extends net.ankiweb.rsdroid.BackendService {

    private Pointer mBackEndPointer = null;

    @Override
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
}
