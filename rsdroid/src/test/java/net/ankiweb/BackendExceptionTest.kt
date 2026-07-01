// SPDX-License-Identifier: GPL-3.0-or-later

package net.ankiweb

import anki.backend.BackendError
import net.ankiweb.rsdroid.BackendException.BackendDbException
import net.ankiweb.rsdroid.BackendException.BackendDbException.BackendDbCorruptException
import net.ankiweb.rsdroid.BackendException.BackendDbException.BackendDbFileTooNewException
import net.ankiweb.rsdroid.BackendException.BackendDbException.BackendDbFileTooOldException
import net.ankiweb.rsdroid.BackendException.BackendDbException.BackendDbFullException
import net.ankiweb.rsdroid.BackendException.BackendDbException.BackendDbLockedException
import net.ankiweb.rsdroid.BackendException.BackendDbException.BackendDbMissingEntityException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests [BackendDbException.fromDbError]: AnkiDroid's startup error handling relies on these types */
class BackendExceptionTest {
    @Test
    fun diskFullIsTyped() {
        val exception = fromDbError("DbError { info: \"SqliteFailure(Error { code: DiskFull })\", kind: Other }")
        assertEquals(BackendDbFullException::class.java, exception.javaClass)
        // existing catch (BackendDbException) sites must still match
        assertTrue(exception is BackendDbException)
    }

    @Test
    fun corruptionIsTyped() {
        val exception = fromDbError("DbError { info: \"SqliteFailure(Error { code: DatabaseCorrupt })\", kind: Other }")
        assertEquals(BackendDbCorruptException::class.java, exception.javaClass)
        assertTrue(exception is BackendDbException)
    }

    @Test
    fun fileTooNewIsTyped() {
        assertEquals(
            BackendDbFileTooNewException::class.java,
            fromDbError("DbError { info: \"\", kind: FileTooNew }").javaClass,
        )
    }

    @Test
    fun fileTooOldIsTyped() {
        assertEquals(
            BackendDbFileTooOldException::class.java,
            fromDbError("DbError { info: \"\", kind: FileTooOld }").javaClass,
        )
    }

    @Test
    fun missingEntityIsTyped() {
        assertEquals(
            BackendDbMissingEntityException::class.java,
            fromDbError("DbError { info: \"\", kind: MissingEntity }").javaClass,
        )
    }

    @Test
    fun lockedIsTyped() {
        assertEquals(
            BackendDbLockedException::class.java,
            fromDbError("Anki already open, or media currently syncing.").javaClass,
        )
    }

    @Test
    fun kindOtherWithoutMarkersIsUntyped() {
        assertEquals(
            BackendDbException::class.java,
            fromDbError("DbError { info: \"something else\", kind: Other }").javaClass,
        )
    }

    @Test
    fun unknownMessageIsUntyped() {
        assertEquals(BackendDbException::class.java, fromDbError("no recognisable marker").javaClass)
    }

    private fun fromDbError(message: String) =
        BackendDbException.fromDbError(
            BackendError
                .newBuilder()
                .setKind(BackendError.Kind.DB_ERROR)
                .setMessage(message)
                .build(),
        )
}
