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

import net.ankiweb.rsdroid.BackendException;

import org.json.JSONArray;

/** The single instance where all data access occurs */
// TODO: This needs to be implemented and used.
public class RustSQLBackend implements SQLHandler {
    // This class exists as the Rust backend uses a single connection for SQLite, rather than a connection pool
    // This means that SQL can occur cross-threads.
    // There are a few problems with this:
    // * When inside a transaction, another thread can add commands, or close the transaction
    // * Commands can either be sent from the Java, or from the Rust.
    // * We have no knowledge about whether a Rust command will start a transaction

    // We handle this using a mutex and some invariants:
    // * If a transaction is held by a thread, have the thread keep the mutex until the transaction is closed
    // * Only one Rust command can run at a time - already true as with_col in rust uses a mutex


    private final SQLHandler mBackend;

    public RustSQLBackend(SQLHandler mBackend) {
        this.mBackend = mBackend;
    }


    public void beginTransaction() {
        mBackend.beginTransaction();
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

    public JSONArray fullQuery(String query, Object[] bindArgs) {
        try {
            return mBackend.fullQuery(query, bindArgs);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public int executeGetRowsAffected(String sql, Object[] bindArgs) {
        try {
            return mBackend.executeGetRowsAffected(sql, bindArgs);
        } catch (BackendException e) {
            throw new RuntimeException(e);
        }
    }

    public long insertForId(String sql, Object[] bindArgs) {
        try {
            return mBackend.insertForId(sql, bindArgs);
        } catch (BackendException e) {
            throw new RuntimeException(e);
        }
    }
}
