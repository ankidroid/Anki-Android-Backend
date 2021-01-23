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

import androidx.annotation.CheckResult;

import org.json.JSONArray;

import BackendProto.Sqlite;

public interface SQLHandler {
    @CheckResult
    JSONArray fullQuery(String query, Object... bindArgs);
    int executeGetRowsAffected(String sql, Object... bindArgs);
    long insertForId(String sql, Object... bindArgs);

    void beginTransaction();
    void commitTransaction();
    void rollbackTransaction();

    @CheckResult
    String[] getColumnNames(String sql);

    void closeDatabase();

    @CheckResult
    String getPath();

    /* Protobuf-related (#6) */
    Sqlite.DBResponse getPage(int page);
    Sqlite.DBResponse fullQueryProto(String query, Object... bindArgs);

    void cancelCurrentProtoQuery();
}
