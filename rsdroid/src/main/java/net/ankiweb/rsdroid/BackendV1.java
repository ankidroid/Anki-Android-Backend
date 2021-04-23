package net.ankiweb.rsdroid;

import net.ankiweb.rsdroid.database.SQLHandler;

import java.io.Closeable;

import BackendProto.Backend;

public interface BackendV1 extends SQLHandler, net.ankiweb.rsdroid.RustBackend, net.ankiweb.rsdroid.Adbackend, Closeable {
    void openAnkiDroidCollection(Backend.OpenCollectionIn args) throws BackendException;

    boolean isOpen();

    /**
     * Downgrades the collection from Schema 16 to Schema 11
     * @param collectionPath The fully qualified path to collection.anki2
     * @throws BackendException The collection is not schema 16
     * @throws BackendException Collection is already open
     */
    void downgradeBackend(String collectionPath) throws BackendException;
}