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
package net.ankiweb.rsdroid

import androidx.annotation.CheckResult
import androidx.annotation.VisibleForTesting
import anki.ankidroid.DbResponse
import anki.backend.BackendError
import anki.backend.BackendInit
import anki.backend.GeneratedBackend
import anki.generic.Int64
import com.google.protobuf.ByteString
import com.google.protobuf.InvalidProtocolBufferException
import net.ankiweb.rsdroid.database.NotImplementedException
import net.ankiweb.rsdroid.database.SQLHandler
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.io.File
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

private val logger = LoggerFactory.getLogger(Backend::class.java)

open class Backend(
    langs: Iterable<String> = listOf("en"),
) : GeneratedBackend(),
    SQLHandler,
    Closeable {
    // Set on init; unset on .close(). Access via withBackend()
    private var backendPointer: Long? = null

    /**
     * AnkiDroid#21455): Ensures [close] cannot free the backend during a [runMethodRaw] call.
     */
    private val backendLock = ReentrantReadWriteLock()

    val tr: Translations by lazy {
        Translations(this)
    }

    fun isOpen(): Boolean = backendPointer != null

    fun openCollection(collectionPath: String) {
        val (mediaFolder, mediaDb) =
            if (collectionPath == ":memory:") {
                listOf("", "")
            } else {
                listOf(
                    collectionPath.replace(".anki2", ".media"),
                    collectionPath.replace(".anki2", ".media.db"),
                )
            }
        openCollection(collectionPath, mediaFolder, mediaDb)
    }

    /** Forces a full media check on next sync. Only valid with new backend. */
    @Suppress("unused") // used in AnkiDroid
    fun removeMediaDb(colPath: String) {
        val file = File(colPath.replace(".anki2", ".media.db"))
        if (file.exists()) {
            file.delete()
        }
    }

    /**
     * Open a backend instance, loading the shared library if not already loaded.
     */
    init {
        logger.debug("Opening rust backend with lang={}", langs)
        val input =
            BackendInit
                .newBuilder()
                .addAllPreferredLangs(langs)
                .build()
                .toByteArray()
        val outBytes = unpackResult(NativeMethods.openBackend(input))
        backendPointer = Int64.parseFrom(outBytes).`val`
    }

    /**
     * Close the backend, and any open collection. This object can not be used after this.
     */
    override fun close() {
        logger.debug("Closing rust backend")
        backendLock.write {
            NativeMethods.closeBackend(backendPointer!!)
            backendPointer = null
        }
    }

    /**
     * Open a collection. There must not already be an open collection.
     */
    override fun openCollection(
        collectionPath: String,
        mediaFolderPath: String,
        mediaDbPath: String,
    ) {
        try {
            super.openCollection(collectionPath, mediaFolderPath, mediaDbPath)
        } catch (exc: BackendException.BackendDbException) {
            throw exc.toSQLiteException("db open")
        }
    }

    /**
     * Closes an open collection. There must be an open collection.
     */
    override fun closeCollection(downgradeToSchema11: Boolean) {
        cancelAllProtoQueries()
        super.closeCollection(downgradeToSchema11)
    }

    /**
     * All backend methods (except for backend init/close) flow through this.
     */
    override fun runMethodRaw(
        service: Int,
        method: Int,
        input: ByteArray,
    ): ByteArray =
        withBackend {
            unpackResult(NativeMethods.runMethodRaw(it, service, method, input))
        }

    /**
     * Run the provided closure with access to the backend.
     * @throws BackendException if backend closed.
     */
    private fun <T> withBackend(fn: (ptr: Long) -> T): T =
        backendLock.read {
            val pointer = backendPointer ?: throw BackendException("Backend has been closed")
            fn(pointer)
        }

    // other DB methods

    override fun closeDatabase(): Unit = throw NotImplementedException("should close collection, not db")

    override fun getPath(): String? = throw NotImplementedException()

    @CheckResult
    override fun fullQuery(
        query: String,
        bindArgs: Array<Any?>?,
    ): JSONArray =
        try {
            fullQueryInternal(query, bindArgs ?: emptyArray())
        } catch (e: JSONException) {
            throw RuntimeException(e)
        }

    @Throws(JSONException::class)
    private fun fullQueryInternal(
        sql: String,
        bindArgs: Array<Any?>,
    ): JSONArray {
        val output = runDbCommand(dbRequestJson(sql, bindArgs)).toStringUtf8()
        return JSONArray(output)
    }

    override fun insertForId(
        sql: String,
        bindArgs: Array<Any?>?,
    ): Long = super.insertForId(dbRequestJson(sql, bindArgs ?: emptyArray()))

    override fun executeGetRowsAffected(
        sql: String,
        bindArgs: Array<Any?>?,
    ): Int = runDbCommandForRowCount(dbRequestJson(sql, bindArgs ?: emptyArray())).toInt()

    // Begin Protobuf-based database streaming methods (#6)
    override fun fullQueryProto(
        query: String,
        bindArgs: Array<out Any?>,
    ): DbResponse = runDbCommandProto(dbRequestJson(query, bindArgs))

    override fun getNextSlice(
        startIndex: Long,
        sequenceNumber: Int,
    ): DbResponse = getNextResultPage(sequenceNumber, startIndex)

    override fun cancelCurrentProtoQuery(sequenceNumber: Int) {
        flushQuery(sequenceNumber)
    }

    override fun cancelAllProtoQueries() {
        flushAllQueries()
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    override fun setPageSize(pageSizeBytes: Long) {
        super.setPageSize(pageSizeBytes)
    }

    override fun getColumnNames(sql: String): Array<String> = getColumnNamesFromQuery(sql).toTypedArray()

    companion object {
        const val MAX_MEDIA_FILENAME_LENGTH = 120

        const val MAX_MEDIA_FILENAME_LENGTH_SERVER = 255

        const val MAX_INDIVIDUAL_MEDIA_FILE_SIZE = 100 * 1024 * 1024L
    }
}

/**
 * Build a JSON DB request
 */
private fun dbRequestJson(
    sql: String = "",
    bindArgs: Array<out Any?> = emptyArray(),
    firstRowOnly: Boolean = false,
): ByteString {
    val o =
        JSONObject().apply {
            put("kind", "query")
            put("sql", sql)
            put("args", JSONArray(bindArgs.toList()))
            put("first_row_only", firstRowOnly)
        }
    return ByteString.copyFromUtf8(o.toString())
}

/**
 * Unpack success/error tuple from backend, and throw if error.
 */
private fun unpackResult(result: Array<ByteArray?>?): ByteArray {
    if (result == null) {
        throw BackendException("null return from backend method")
    }
    val (successBytes, errorBytes) = result
    if (errorBytes != null) {
        // convert the error to an exception
        val pbError: BackendError =
            try {
                BackendError.parseFrom(errorBytes)
            } catch (invalidProtocolBufferException: InvalidProtocolBufferException) {
                throw BackendException.fromException(invalidProtocolBufferException)
            }
        throw BackendException.fromError(pbError)
    } else if (successBytes != null) {
        return successBytes
    } else {
        // should not happen
        throw BackendException("both ok & err cases null")
    }
}
