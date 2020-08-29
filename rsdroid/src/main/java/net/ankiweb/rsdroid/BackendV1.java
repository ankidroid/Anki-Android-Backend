package net.ankiweb.rsdroid;

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
}
