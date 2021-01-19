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

package net.ankiweb.rsdroid.database;

import org.json.JSONArray;

import java.util.Stack;

import BackendProto.Sqlite;

/** Handles transaction state management */
public class Session implements SQLHandler {
    private final SQLHandler mBackend;
    private final Stack<SessionState> mSessions = new Stack<>();

    public Session(SQLHandler backend) {
        this.mBackend = backend;
    }

    private boolean mInTransaction() {
        return !mSessions.empty();
    }

    public void beginTransaction() {
        if (!mInTransaction()) {
            mBackend.beginTransaction();
        }
        mSessions.add(SessionState.initial());
    }

    @Override
    public void commitTransaction() {
        mBackend.commitTransaction();
    }

    @Override
    public void rollbackTransaction() {
        mBackend.rollbackTransaction();
    }

    @Override
    public String[] getColumnNames(String sql) {
        return mBackend.getColumnNames(sql);
    }

    @Override
    public void closeDatabase() {
        mBackend.closeDatabase();
    }

    @Override
    public String getPath() {
        return mBackend.getPath();
    }

    @Override
    public Sqlite.DBResponse getPage(int page) {
        return mBackend.getPage(page);
    }

    @Override
    public Sqlite.DBResponse fullQueryProto(String query, Object... bindArgs) {
        return mBackend.fullQueryProto(query, bindArgs);
    }

    @Override
    public int getCurrentRowCount() {
        return mBackend.getCurrentRowCount();
    }

    @Override
    public void cancelCurrentProtoQuery() {
        mBackend.cancelCurrentProtoQuery();
    }


    public void setTransactionSuccessful() {
        if (!inTransaction()) {
            throw new IllegalStateException("must be in a transaction");
        }
        mSessions.peek().markSuccessful();
    }

    public void endTransaction() {
        if (!inTransaction()) {
            throw new IllegalStateException("must be in a transaction");
        }

        SessionState currentState = pop();

        if (mSessions.size() != 0) {
            if (!currentState.isSuccessful()) {
                mSessions.peek().markAsFailed();
            }

            return;
        }

        // We have a single session - rollback or abort

        if (currentState.isSuccessful()) {
            commitTransaction();
        } else {
            rollbackTransaction();
        }
    }

    private SessionState pop() {
        return mSessions.pop();
    }

    public boolean inTransaction() {
        return mInTransaction();
    }

    public JSONArray fullQuery(String query, Object[] bindArgs) {
        return mBackend.fullQuery(query, bindArgs);
    }

    @Override
    public int executeGetRowsAffected(String sql, Object[] bindArgs) {
        return mBackend.executeGetRowsAffected(sql, bindArgs);
    }

    @Override
    public long insertForId(String sql, Object[] bindArgs) {
        return mBackend.insertForId(sql, bindArgs);
    }

    public static class SessionState {
        private boolean mTransactionMarkedSuccessful;
        private boolean mIsFailed;

        public SessionState(boolean success, boolean isFailed) {
            mTransactionMarkedSuccessful = success;
            mIsFailed = isFailed;
        }

        public static SessionState initial() {
            return new SessionState(false, false);
        }

        public boolean isSuccessful() {
            return isMarkedSuccessful() && !isFailed();
        }

        public boolean isMarkedSuccessful() {
            return mTransactionMarkedSuccessful;
        }

        public boolean isFailed() {
            return mIsFailed;
        }

        public void markAsFailed() {
            mIsFailed = true;
        }

        public void markSuccessful() {
            mTransactionMarkedSuccessful = true;
        }
    }
}
