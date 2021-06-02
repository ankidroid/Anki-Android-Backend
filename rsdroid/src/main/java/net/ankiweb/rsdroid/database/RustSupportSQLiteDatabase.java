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
 *     Copyright (C) 2006 The Android Open Source Project
 *     Copyright (C) 2016 The Android Open Source Project
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
 * from android.database.sqlite.SQLiteStatement
 * from update/insert/delete
 */

package net.ankiweb.rsdroid.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteTransactionListener;
import android.os.CancellationSignal;
import android.util.Pair;

import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteQuery;
import androidx.sqlite.db.SupportSQLiteStatement;

import net.ankiweb.rsdroid.BackendV1;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import static android.text.TextUtils.isEmpty;

public class RustSupportSQLiteDatabase implements SupportSQLiteDatabase {
    private static final String[] CONFLICT_VALUES = new String[]
            {"", " OR ROLLBACK ", " OR ABORT ", " OR FAIL ", " OR IGNORE ", " OR REPLACE "};

    private final ThreadLocal<Session> sessionFactory;
    private final boolean isReadOnly;
    private boolean isOpen;

    public RustSupportSQLiteDatabase(BackendV1 backend, boolean readOnly) {
        if (backend == null) {
            throw new IllegalArgumentException("backend was null");
        }
        this.sessionFactory = new SessionThreadLocal(backend);
        this.isReadOnly = readOnly;
        this.isOpen = true;
    }

    @Override
    public boolean isReadOnly() {
        return isReadOnly;
    }

    @Override
    public boolean isOpen() {
        return isOpen;
    }

    @Override
    public SupportSQLiteStatement compileStatement(String sql) {
        return new RustSQLiteStatement(this, sql);
    }

    @Override
    public void beginTransaction() {
        getSession().beginTransaction();
    }

    @Override
    public void endTransaction() {
        getSession().endTransaction();
    }

    @Override
    public void setTransactionSuccessful() {
        getSession().setTransactionSuccessful();
    }

    @Override
    public boolean inTransaction() {
        return getSession().inTransaction();
    }

    @Override
    public int getVersion() {
        throw NotImplementedException.todo();
    }

    @Override
    public void setVersion(int version) {
        throw NotImplementedException.todo();
    }

    @Override
    public Cursor query(String query) {
        return query(query, null);
    }

    @Override
    public Cursor query(String query, Object[] bindArgs) {
        return new StreamingProtobufSQLiteCursor(getSession(), query, bindArgs);
    }


    @Override
    public Cursor query(SupportSQLiteQuery query) {
        throw NotImplementedException.todo();
    }

    @Override
    public Cursor query(SupportSQLiteQuery query, CancellationSignal cancellationSignal) {
        throw NotImplementedException.todo();
    }

    @Override
    public long insert(String table, int conflictAlgorithm, ContentValues values) throws SQLException {
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT");
        sql.append(CONFLICT_VALUES[conflictAlgorithm]);
        sql.append(" INTO ");
        sql.append(table);
        sql.append('(');

        Object[] bindArgs = null;
        int size = (values != null && values.size() > 0)
                ? values.size() : 0;
        if (size > 0) {
            bindArgs = new Object[size];
            int i = 0;
            for (Map.Entry<String, Object> entry : values.valueSet()) {
                sql.append((i > 0) ? "," : "");
                sql.append(entry.getKey());
                bindArgs[i++] = entry.getValue();
            }
            sql.append(')');
            sql.append(" VALUES (");
            for (i = 0; i < size; i++) {
                sql.append((i > 0) ? ",?" : "?");
            }
        } else {
            sql.append((String) null).append(") VALUES (NULL");
        }
        sql.append(')');

        query(sql.toString(), bindArgs).close();
        return 0;
    }

    @Override
    public int update(String table, int conflictAlgorithm, ContentValues values, String whereClause, Object[] whereArgs) {
        // taken from SQLiteDatabase class.
        if (values == null || values.size() == 0) {
            throw new IllegalArgumentException("Empty values");
        }
        StringBuilder sql = new StringBuilder(120);
        sql.append("UPDATE ");
        sql.append(CONFLICT_VALUES[conflictAlgorithm]);
        sql.append(table);
        sql.append(" SET ");

        // move all bind args to one array
        int setValuesSize = values.size();
        int bindArgsSize = (whereArgs == null) ? setValuesSize : (setValuesSize + whereArgs.length);
        Object[] bindArgs = new Object[bindArgsSize];
        int i = 0;
        for (String colName : values.keySet()) {
            sql.append((i > 0) ? "," : "");
            sql.append(colName);
            bindArgs[i++] = values.get(colName);
            sql.append("=?");
        }
        if (whereArgs != null) {
            for (i = setValuesSize; i < bindArgsSize; i++) {
                bindArgs[i] = whereArgs[i - setValuesSize];
            }
        }
        if (!isEmpty(whereClause)) {
            sql.append(" WHERE ");
            sql.append(whereClause);
        }

        return this.executeGetRowsAffected(sql.toString(), bindArgs);
    }

    @Override
    public void execSQL(String sql) throws SQLException {
        execSQL(sql, null);
    }

    @Override
    public void execSQL(String sql, Object[] bindArgs) throws SQLException {
        query(sql, bindArgs).close();
    }

    @Override
    public boolean needUpgrade(int newVersion) {
        // needed for metaDB, but not for Anki DB
        throw NotImplementedException.todo();
    }

    @Override
    public String getPath() {
        return getSession().getPath();
    }


    @Override
    public boolean isWriteAheadLoggingEnabled() {
        return false;
    }

    @Override
    public void disableWriteAheadLogging() {
        // Nothing to do - openAnkiDroidCollection does this
    }

    @Override
    public boolean isDatabaseIntegrityOk() {
        Cursor pragma_integrity_check = query("pragma integrity_check");
        if (!pragma_integrity_check.moveToFirst()) {
            return false;
        }
        String value = pragma_integrity_check.getString(0);
        return "ok".equals(value);
    }

    @Override
    public void close() {
        isOpen = false;
        getSession().closeDatabase();
    }

    /* Not part of interface */

    public int executeGetRowsAffected(String sql, Object[] bindArgs) {
        try {
            return getSession().executeGetRowsAffected(sql, bindArgs);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public long insertForForId(String sql, Object[] bindArgs) {
        try {
            return getSession().insertForId(sql, bindArgs);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** Helper methods */

    private Session getSession() {
        return sessionFactory.get();
    }

    /** Confirmed that the below are not used for our code */


    @Override
    public int delete(String table, String whereClause, Object[] whereArgs) {
        // Complex to implement and not required
        throw new NotImplementedException();
    }

    @Override
    public boolean isDbLockedByCurrentThread() {
        throw new NotImplementedException();
    }

    @Override
    public boolean yieldIfContendedSafely() {
        throw new NotImplementedException();
    }

    @Override
    public boolean yieldIfContendedSafely(long sleepAfterYieldDelay) {
        throw new NotImplementedException();
    }

    @Override
    public void setLocale(Locale locale) {
        throw new NotImplementedException();
    }

    @Override
    public void setMaxSqlCacheSize(int cacheSize) {
        throw new NotImplementedException();
    }

    @Override
    public long getMaximumSize() {
        throw new NotImplementedException();
    }

    @Override
    public long setMaximumSize(long numBytes) {
        throw new NotImplementedException();
    }

    @Override
    public long getPageSize() {
        throw new NotImplementedException();
    }

    @Override
    public void setPageSize(long numBytes) {
        throw new NotImplementedException();
    }


    @Override
    public void setForeignKeyConstraintsEnabled(boolean enable) {
        throw new NotImplementedException();
    }


    @Override
    public boolean enableWriteAheadLogging() {
        throw new NotImplementedException();
    }

    @Override
    public List<Pair<String, String>> getAttachedDbs() {
        throw new NotImplementedException();
    }

    @Override
    public void beginTransactionNonExclusive() {
        throw new NotImplementedException();
    }

    @Override
    public void beginTransactionWithListener(SQLiteTransactionListener transactionListener) {
        throw new NotImplementedException();
    }

    @Override
    public void beginTransactionWithListenerNonExclusive(SQLiteTransactionListener transactionListener) {
        throw new NotImplementedException();
    }
}
