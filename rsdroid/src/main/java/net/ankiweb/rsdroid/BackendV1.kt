package net.ankiweb.rsdroid

import anki.ankidroid.GetActiveSequenceNumbersResponse
import anki.scheduler.SchedTimingTodayResponse
import net.ankiweb.rsdroid.database.SQLHandler
import java.io.Closeable

/**
 * This interface describes valid methods when running the backend in the legacy schema 11
 * mode.
 */
interface BackendV1 : SQLHandler, Closeable {
    fun openCollection(collectionPath: String, mediaFolderPath: String, mediaDbPath: String, logPath: String, forceSchema11: Boolean);
    // schema11
    fun openCollection(collectionPath: String) {
        openCollection(collectionPath, "", "", "", true)
    }
    fun closeCollection(downgrade: Boolean = false);

    fun schedTimingTodayLegacy(createdSecs: Long, createdMinsWest: Int, nowSecs: Long, nowMinsWest: Int, rolloverHour: Int): SchedTimingTodayResponse;
    fun getActiveSequenceNumbers(): GetActiveSequenceNumbersResponse


    /**
     * Whether the backend (not collection) is open. Not really useful outside tests.
     */
    fun isOpen(): Boolean
}
