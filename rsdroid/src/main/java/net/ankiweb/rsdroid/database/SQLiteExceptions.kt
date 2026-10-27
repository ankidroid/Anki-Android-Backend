// SPDX-License-Identifier: GPL-3.0-or-later

package net.ankiweb.rsdroid.database

import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabaseCorruptException
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteFullException
import net.ankiweb.rsdroid.BackendException
import net.ankiweb.rsdroid.BackendException.BackendDbException
import net.ankiweb.rsdroid.BackendException.BackendDbException.BackendDbCorruptException
import net.ankiweb.rsdroid.BackendException.BackendDbException.BackendDbFullException
import java.util.Locale
import java.util.regex.Pattern

/**
 * Maps a [BackendException] to the android.database.sqlite exception the framework
 * SQLite implementation would have thrown, keeping the androidx.sqlite bridge
 * behaviour-compatible. Formerly `BackendException.toSQLiteException`; it lives
 * with the bridge so that :rsdroid has no Android dependencies.
 */
fun BackendException.toSQLiteException(query: String): RuntimeException =
    when (this) {
        is BackendDbFullException -> SQLiteFullException(localizedMessage)
        is BackendDbCorruptException ->
            SQLiteDatabaseCorruptException(
                String.format(Locale.ROOT, "error while compiling: \"%s\": %s", query, localizedMessage),
            )
        is BackendDbException -> toDbSQLiteException(query)
        else ->
            SQLiteException(
                String.format(Locale.ROOT, "error while compiling: \"%s\": %s", query, localizedMessage),
                this,
            )
    }

private fun BackendDbException.toDbSQLiteException(query: String): RuntimeException {
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
            val errorMessage =
                String.format(
                    Locale.ROOT,
                    "Cannot bind argument at index %d because the index is out of range.  The statement has %d parameters.",
                    givenParams,
                    expectedParams,
                )
            return IllegalArgumentException(errorMessage, this)
        }
    } else if (message.contains("ConstraintViolation")) {
        return SQLiteConstraintException(message)
    }
    val outMessage = String.format(Locale.ROOT, "error while compiling: \"%s\": %s", query, message)
    return SQLiteException(outMessage, this)
}
