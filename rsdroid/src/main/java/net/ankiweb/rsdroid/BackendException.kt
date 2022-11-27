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

import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabaseCorruptException
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteFullException
import anki.backend.BackendError
import net.ankiweb.rsdroid.exceptions.*
import net.ankiweb.rsdroid.exceptions.BackendSyncException.BackendSyncAuthFailedException
import java.util.*
import java.util.regex.Pattern

open class BackendException : RuntimeException {
    private val error: BackendError?

    constructor(error: BackendError) : super(error.message) {
        this.error = error
    }

    constructor(message: String?) : super(message) {
        error = null
    }

    open fun toSQLiteException(query: String): RuntimeException {
        val message = String.format(Locale.ROOT, "error while compiling: \"%s\": %s", query, this.localizedMessage)
        return SQLiteException(message, this)
    }

    class BackendDbException(error: BackendError) : BackendException(error) {
        override fun toSQLiteException(query: String): RuntimeException {
            val message = this.localizedMessage
            if (message == null) {
                val outMessage = String.format(Locale.ROOT, "Unknown error while compiling: \"%s\"", query)
                return SQLiteException(outMessage, this)
            }
            if (message.contains("InvalidParameterCount")) {
                val p = Pattern.compile("InvalidParameterCount\\((\\d*), (\\d*)\\)").matcher(message)
                if (p.find()) {
                    val givenParams = p.group(1)!!.toInt()
                    val expectedParams = p.group(2)!!.toInt()
                    val errorMessage = String.format(Locale.ROOT, "Cannot bind argument at index %d because the index is out of range.  The statement has %d parameters.", givenParams, expectedParams)
                    return IllegalArgumentException(errorMessage, this)
                }
            } else if (message.contains("ConstraintViolation")) {
                return SQLiteConstraintException(message)
            } else if (message.contains("DiskFull")) {
                return SQLiteFullException(message)
            } else if (message.contains("DatabaseCorrupt")) {
                val outMessage = String.format(Locale.ROOT, "error while compiling: \"%s\": %s", query, message)
                return SQLiteDatabaseCorruptException(outMessage)
            }
            val outMessage = String.format(Locale.ROOT, "error while compiling: \"%s\": %s", query, message)
            return SQLiteException(outMessage, this)
        }

        class BackendDbFileTooNewException(error: BackendError) : BackendException(error)
        class BackendDbFileTooOldException(error: BackendError) : BackendException(error)
        class BackendDbLockedException(error: BackendError) : BackendException(error)
        class BackendDbMissingEntityException(error: BackendError) : BackendException(error)
        companion object {
            fun fromDbError(error: BackendError): BackendException {
                val localised = error.message ?: return BackendDbException(error)
                if (localised.contains("kind: FileTooNew")) {
                    return BackendDbFileTooNewException(error)
                }
                if (localised.contains("kind: FileTooOld")) {
                    return BackendDbFileTooOldException(error)
                }
                if (localised.contains("kind: MissingEntity")) {
                    return BackendDbMissingEntityException(error)
                }
                if (localised.contains("kind: Other")) {
                    return BackendDbException(error)
                }
                // Anki already open, or media currently syncing.
                return if (localised.startsWith("Anki already open")) {
                    BackendDbLockedException(error)
                } else BackendDbException(error)
            }
        }
    }

    class BackendSearchException(error: BackendError) : BackendException(error)
    class BackendFatalError(error: BackendError) : BackendException(error)

    companion object {
        fun fromError(error: BackendError): BackendException {
            when (error.kind!!) {
                BackendError.Kind.DB_ERROR -> return BackendDbException.fromDbError(error)
                BackendError.Kind.JSON_ERROR -> return BackendJsonException(error)
                BackendError.Kind.SYNC_AUTH_ERROR -> return BackendSyncAuthFailedException(error)
                BackendError.Kind.SYNC_OTHER_ERROR -> return BackendSyncException(error)
                BackendError.Kind.FATAL_ERROR -> return BackendFatalError(error)
                BackendError.Kind.EXISTS -> return BackendExistingException(error)
                BackendError.Kind.FILTERED_DECK_ERROR -> return BackendDeckIsFilteredException(error)
                BackendError.Kind.INTERRUPTED -> return BackendInterruptedException(error)
                BackendError.Kind.PROTO_ERROR -> return BackendProtoException(error)
                BackendError.Kind.NOT_FOUND_ERROR -> return BackendNotFoundException(error)
                BackendError.Kind.INVALID_INPUT -> return BackendInvalidInputException.fromInvalidInputError(error)
                BackendError.Kind.NETWORK_ERROR -> return BackendNetworkException(error)
                BackendError.Kind.TEMPLATE_PARSE -> return BackendTemplateException.fromTemplateError(error)
                BackendError.Kind.IO_ERROR -> return BackendIoException(error)
                BackendError.Kind.SEARCH_ERROR -> return BackendSearchException(error)

                BackendError.Kind.UNDO_EMPTY -> return BackendException(error)
                BackendError.Kind.CUSTOM_STUDY_ERROR -> return BackendException(error)
                BackendError.Kind.IMPORT_ERROR -> return BackendException(error)
                BackendError.Kind.DELETED -> return BackendException(error)
                BackendError.Kind.CARD_TYPE_ERROR -> return BackendException(error)
                BackendError.Kind.UNRECOGNIZED -> return BackendException(error)
            }
        }

        fun fromException(ex: Exception?): RuntimeException {
            return RuntimeException(ex)
        }
    }
}
