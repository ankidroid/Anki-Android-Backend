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

package net.ankiweb.rsdroid.database;

import android.database.Cursor;
import android.database.sqlite.SQLiteDoneException;

import androidx.sqlite.db.SupportSQLiteStatement;

import java.util.HashMap;
import java.util.Set;

public class RustSQLiteStatement implements SupportSQLiteStatement {
    private final RustSupportSQLiteDatabase database;
    private final String sql;

    private final HashMap<Integer, Object> mBindings = new HashMap<>();

    public RustSQLiteStatement(RustSupportSQLiteDatabase database, String sql) {
        this.database = database;
        this.sql = sql;
    }

    @Override
    public void execute() {
        database.query(sql, getBindings());
    }

    @Override
    public int executeUpdateDelete() {
        return database.executeGetRowsAffected(sql, getBindings());
    }

    @Override
    public long executeInsert() {
        return database.insertForForId(sql, getBindings());
    }

    @Override
    public long simpleQueryForLong() {
        try (Cursor query = database.query(sql, getBindings())) {
            if (!query.moveToFirst()) {
                throw new SQLiteDoneException();
            }
            return query.getLong(0);
        }
    }

    @Override
    public String simpleQueryForString() {
        try (Cursor query = database.query(sql, getBindings())) {
            if (!query.moveToFirst()) {
                throw new SQLiteDoneException();
            }
            return query.getString(0);
        }
    }

    @Override
    public void bindNull(int index) {
        bind(index, null);
    }

    @Override
    public void bindLong(int index, long value) {
        bind(index, value);
    }

    @Override
    public void bindDouble(int index, double value) {
        bind(index, value);
    }

    @Override
    public void bindString(int index, String value) {
        bind(index, value);
    }

    @Override
    public void bindBlob(int index, byte[] value) {
        bind(index, value);
    }

    @Override
    public void clearBindings() {
        mBindings.clear();
    }

    @Override
    public void close() {

    }


    private void bind(int index, Object value) {
        mBindings.put(index, value);
    }

    Object[] getBindings() {
        int max = max(mBindings.keySet());

        Object[] ret = new Object[max + 1];
        for (int i = 0; i <= max; i++) {
            if (mBindings.containsKey(i)) {
                ret[i] = mBindings.get(i);
            }
        }
        return ret;
    }

    private int max(Set<Integer> integerSet) {
        int max = -1;
        for (int i : integerSet) {
            max = Math.max(max, i);
        }
        return max;
    }
}
