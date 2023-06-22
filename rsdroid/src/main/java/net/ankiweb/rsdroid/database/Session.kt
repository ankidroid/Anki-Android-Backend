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
 *
 * Contains code under the following license
 *
 *     Copyright (C) 2011 The Android Open Source Project
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 *     From android.database.sqlite.SQLiteSession
 */
package net.ankiweb.rsdroid.database

import anki.ankidroid.DbResponse
import org.json.JSONArray
import java.util.*

/** Handles transaction state management  */
class Session(private val backend: SQLHandler) : SQLHandler {
    private val sessions = Stack<SessionState>()
    private fun mInTransaction(): Boolean {
        return !sessions.empty()
    }

    override fun beginTransaction() {
        if (!mInTransaction()) {
            backend.beginTransaction()
        }
        sessions.add(SessionState.initial())
    }

    override fun commitTransaction() {
        backend.commitTransaction()
    }

    override fun rollbackTransaction() {
        backend.rollbackTransaction()
    }

    override fun getColumnNames(sql: String): Array<String> {
        return backend.getColumnNames(sql)
    }

    override fun closeDatabase() {
        backend.closeDatabase()
    }

    override fun getPath(): String? {
        return backend.getPath()
    }

    override fun getNextSlice(startIndex: Long, sequenceNumber: Int): DbResponse {
        return backend.getNextSlice(startIndex, sequenceNumber)
    }

    override fun fullQueryProto(query: String, bindArgs: Array<Any?>?): DbResponse {
        return backend.fullQueryProto(query, bindArgs)
    }

    override fun cancelCurrentProtoQuery(sequenceNumber: Int) {
        backend.cancelCurrentProtoQuery(sequenceNumber)
    }

    override fun cancelAllProtoQueries() {
        backend.cancelAllProtoQueries()
    }

    override fun setPageSize(pageSizeBytes: Long) {
        backend.setPageSize(pageSizeBytes)
    }

    fun setTransactionSuccessful() {
        check(inTransaction()) { "must be in a transaction" }
        sessions.peek().markSuccessful()
    }

    fun endTransaction() {
        check(inTransaction()) { "must be in a transaction" }
        val currentState = pop()
        if (sessions.size != 0) {
            if (!currentState.isSuccessful) {
                sessions.peek().markAsFailed()
            }
            return
        }

        // We have a single session - rollback or abort
        if (currentState.isSuccessful) {
            commitTransaction()
        } else {
            rollbackTransaction()
        }
    }

    private fun pop(): SessionState {
        return sessions.pop()
    }

    fun inTransaction(): Boolean {
        return mInTransaction()
    }

    override fun fullQuery(query: String, bindArgs: Array<Any?>?): JSONArray {
        return backend.fullQuery(query, bindArgs)
    }

    override fun executeGetRowsAffected(sql: String, bindArgs: Array<Any?>?): Int {
        return backend.executeGetRowsAffected(sql, bindArgs)
    }

    override fun insertForId(sql: String, bindArgs: Array<Any?>?): Long {
        return backend.insertForId(sql, bindArgs)
    }

    class SessionState(var isMarkedSuccessful: Boolean, var isFailed: Boolean) {
        val isSuccessful: Boolean
            get() = isMarkedSuccessful && !isFailed

        fun markAsFailed() {
            isFailed = true
        }

        fun markSuccessful() {
            isMarkedSuccessful = true
        }

        companion object {
            fun initial(): SessionState {
                return SessionState(false, false)
            }
        }
    }
}