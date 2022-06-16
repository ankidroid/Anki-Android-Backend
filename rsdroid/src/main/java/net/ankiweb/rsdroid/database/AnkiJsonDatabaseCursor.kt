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
package net.ankiweb.rsdroid.database

import org.json.JSONArray
import org.json.JSONException

abstract class AnkiJsonDatabaseCursor(private val backend: Session, protected val query: String, protected val bindArgs: Array<Any>) : AnkiDatabaseCursor() {
    protected var results: JSONArray? = null
    private var columnMapping: Array<String>? = null

    // There's no need to close this cursor.
    override fun isClosed(): Boolean {
        return true
    }

    override fun getColumnCount(): Int {
        return if (results!!.length() == 0) {
            0
        } else {
            try {
                results!!.getJSONArray(0).length()
            } catch (e: JSONException) {
                0
            }
        }
    }

    override fun getString(columnIndex: Int): String? {
        return try {
            rowAtCurrentPosition.getString(columnIndex)
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    override fun getShort(columnIndex: Int): Short {
        return try {
            rowAtCurrentPosition.getInt(columnIndex).toShort()
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    override fun getInt(columnIndex: Int): Int {
        return try {
            val rowAtCurrentPosition = rowAtCurrentPosition
            if (rowAtCurrentPosition.isNull(columnIndex)) {
                0
            } else rowAtCurrentPosition.getInt(columnIndex)
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    override fun getLong(columnIndex: Int): Long {
        return try {
            val rowAtCurrentPosition = rowAtCurrentPosition
            if (rowAtCurrentPosition.isNull(columnIndex)) {
                0
            } else rowAtCurrentPosition.getLong(columnIndex)
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    override fun getFloat(columnIndex: Int): Float {
        return try {
            val rowAtCurrentPosition = rowAtCurrentPosition
            if (rowAtCurrentPosition.isNull(columnIndex)) {
                0.0f
            } else rowAtCurrentPosition.getDouble(columnIndex).toFloat()
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    override fun getDouble(columnIndex: Int): Double {
        return try {
            if (rowAtCurrentPosition.isNull(columnIndex)) {
                0.0
            } else {
                rowAtCurrentPosition.getDouble(columnIndex)
            }
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    override fun isNull(columnIndex: Int): Boolean {
        return try {
            rowAtCurrentPosition.isNull(columnIndex)
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    override fun getColumnIndex(columnName: String): Int {
        try {
            val names = columnNames
            for (i in names.indices) {
                if (columnName == names[i]) {
                    return i
                }
            }
        } catch (e: Exception) {
            return -1
        }
        return -1
    }

    @Throws(IllegalArgumentException::class)
    override fun getColumnIndexOrThrow(columnName: String): Int {
        try {
            val names = columnNames
            for (i in names.indices) {
                if (columnName == names[i]) {
                    return i
                }
            }
        } catch (e: Exception) {
            throw IllegalArgumentException(e)
        }
        throw IllegalArgumentException(String.format("Could not find column '%s'", columnName))
    }

    override fun getColumnName(columnIndex: Int): String {
        return columnNamesInternal[columnIndex]
    }

    override fun getColumnNames(): Array<String> {
        return columnNamesInternal
    }

    private val columnNamesInternal: Array<String>
        private get() {
            if (columnMapping == null) {
                columnMapping = backend.getColumnNames(query)
                checkNotNull(columnMapping) { "unable to obtain column mapping" }
            }
            return columnMapping!!
        }

    @get:Throws(JSONException::class)
    protected abstract val rowAtCurrentPosition: JSONArray
    protected fun fullQuery(query: String, bindArgs: Array<Any?>?): JSONArray {
        return backend.fullQuery(query, bindArgs)
    }

    override fun getType(columnIndex: Int): Int {
        throw NotImplementedException()
    }
}