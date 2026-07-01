// SPDX-License-Identifier: GPL-3.0-or-later

package net.ankiweb

import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabaseCorruptException
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteFullException
import androidx.test.ext.junit.runners.AndroidJUnit4
import anki.backend.BackendError
import net.ankiweb.rsdroid.BackendException
import net.ankiweb.rsdroid.BackendException.BackendDbException
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for [BackendException.toSQLiteException]. */
@RunWith(AndroidJUnit4::class) // Robolectric - the stub android.jar can't construct the exceptions
class SQLiteExceptionMappingTest {
    @Test
    fun diskFullMapsToSQLiteFullException() {
        val mapped = fromDbError("DbError { info: \"DiskFull\", kind: Other }").toSQLiteException("select 1")
        assertEquals(SQLiteFullException::class.java, mapped.javaClass)
    }

    @Test
    fun corruptionMapsToSQLiteDatabaseCorruptException() {
        val message = "DbError { info: \"DatabaseCorrupt\", kind: Other }"
        val mapped = fromDbError(message).toSQLiteException("select 1")
        assertEquals(SQLiteDatabaseCorruptException::class.java, mapped.javaClass)
        assertEquals("error while compiling: \"select 1\": $message", mapped.message)
    }

    @Test
    fun constraintViolationMapsToSQLiteConstraintException() {
        val mapped = fromDbError("ConstraintViolation oops").toSQLiteException("insert into x")
        assertEquals(SQLiteConstraintException::class.java, mapped.javaClass)
    }

    @Test
    fun invalidParameterCountMapsToIllegalArgumentException() {
        val mapped = fromDbError("InvalidParameterCount(1, 2)").toSQLiteException("select ?")
        assertEquals(IllegalArgumentException::class.java, mapped.javaClass)
        assertEquals(
            "Cannot bind argument at index 1 because the index is out of range.  The statement has 2 parameters.",
            mapped.message,
        )
    }

    @Test
    fun unknownDbErrorMapsToSQLiteException() {
        val mapped = fromDbError("something unrecognised").toSQLiteException("select 1")
        assertEquals(SQLiteException::class.java, mapped.javaClass)
        assertEquals("error while compiling: \"select 1\": something unrecognised", mapped.message)
    }

    @Test
    fun nonDbBackendExceptionMapsToSQLiteException() {
        val mapped = BackendException("boom").toSQLiteException("select 1")
        assertEquals(SQLiteException::class.java, mapped.javaClass)
        assertEquals("error while compiling: \"select 1\": boom", mapped.message)
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
