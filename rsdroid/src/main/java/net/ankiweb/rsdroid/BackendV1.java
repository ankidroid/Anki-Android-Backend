package net.ankiweb.rsdroid;

import net.ankiweb.rsdroid.database.SQLHandler;

import BackendProto.Backend;

public interface BackendV1 extends SQLHandler, net.ankiweb.rsdroid.RustBackend {
    void openAnkiDroidCollection(Backend.OpenCollectionIn args) throws BackendException;
}