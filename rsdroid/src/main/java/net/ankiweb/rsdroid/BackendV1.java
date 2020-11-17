package net.ankiweb.rsdroid;

import net.ankiweb.rsdroid.database.SQLHandler;

import java.io.Closeable;

import BackendProto.Backend;

public interface BackendV1 extends SQLHandler, net.ankiweb.rsdroid.RustBackend, Closeable {
    void openAnkiDroidCollection(Backend.OpenCollectionIn args) throws BackendException;

    boolean isOpen();
}