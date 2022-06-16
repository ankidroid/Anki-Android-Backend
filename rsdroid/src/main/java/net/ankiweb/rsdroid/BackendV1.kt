package net.ankiweb.rsdroid

import net.ankiweb.rsdroid.database.SQLHandler
import java.io.Closeable

interface BackendV1 : SQLHandler, Closeable {
    fun openCollection(collectionPath: String, mediaFolderPath: String, mediaDbPath: String, logPath: String, forceSchema11: Boolean);
    fun isOpen(): Boolean
}
