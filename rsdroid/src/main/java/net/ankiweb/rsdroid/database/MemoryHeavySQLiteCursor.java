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
import net.ankiweb.rsdroid.RustV1Cleanup;

import org.json.JSONArray;
import org.json.JSONException;

@RustV1Cleanup("To be deleted once protobuf-based cursors are confirmed to be stable")
public class MemoryHeavySQLiteCursor extends AnkiJsonDatabaseCursor {

    private int mPosition = -1;

    public MemoryHeavySQLiteCursor(Session backend, String query, Object[] bindArgs) {
        super(backend, query, bindArgs);
        try {
            mResults = backend.fullQuery(query, bindArgs);
        } catch (BackendException e) {
            throw e.toSQLiteException(query);
        }
    }

    @Override
    public int getCount() {
        return mResults.length();
    }

    @Override
    public boolean moveToFirst() {
        if (mResults.length() == 0) {
            return false;
        }
        mPosition = 0;
        return true;
    }

    @Override
    public boolean moveToNext() {
        mPosition++;
        return mPosition < mResults.length();
    }

    @Override
    public int getPosition() {
        return mPosition;
    }

    @Override
    public void close() {

    }

    @Override
    protected JSONArray getRowAtCurrentPosition() throws JSONException {
        return mResults.getJSONArray(mPosition);
    }
}
