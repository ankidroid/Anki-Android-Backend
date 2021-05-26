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

import org.json.JSONArray;
import org.json.JSONException;

public abstract class AnkiJsonDatabaseCursor extends AnkiDatabaseCursor {

    private final Session backend;
    protected final String query;
    protected final Object[] bindArgs;
    protected JSONArray results;
    private String[] columnMapping;


    public AnkiJsonDatabaseCursor(Session backend, String query, Object[] bindArgs) {
        this.backend = backend;
        this.query = query;
        this.bindArgs = bindArgs;
    }

    // There's no need to close this cursor.
    @Override
    public boolean isClosed() {
        return true;
    }


    @Override
    public int getColumnCount() {
        if (results.length() == 0) {
            return 0;
        } else {
            try {
                return results.getJSONArray(0).length();
            } catch (JSONException e) {
                return 0;
            }
        }
    }

    @Override
    public String getString(int columnIndex) {
        try {
            return getRowAtCurrentPosition().getString(columnIndex);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public short getShort(int columnIndex) {
        try {
            return (short) getRowAtCurrentPosition().getInt(columnIndex);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getInt(int columnIndex) {
        try {
            JSONArray rowAtCurrentPosition = getRowAtCurrentPosition();
            if (rowAtCurrentPosition.isNull(columnIndex)) {
                return 0;
            }
            return rowAtCurrentPosition.getInt(columnIndex);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public long getLong(int columnIndex) {
        try {
            JSONArray rowAtCurrentPosition = getRowAtCurrentPosition();
            if (rowAtCurrentPosition.isNull(columnIndex)) {
                return 0;
            }
            return rowAtCurrentPosition.getLong(columnIndex);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public float getFloat(int columnIndex) {
        try {
            JSONArray rowAtCurrentPosition = getRowAtCurrentPosition();
            if (rowAtCurrentPosition.isNull(columnIndex)) {
                return 0.0f;
            }
            return (float) rowAtCurrentPosition.getDouble(columnIndex);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public double getDouble(int columnIndex) {
        try {
            JSONArray rowAtCurrentPosition = getRowAtCurrentPosition();
            if (rowAtCurrentPosition.isNull(columnIndex)) {
                return 0.0f;
            }
            return rowAtCurrentPosition.getDouble(columnIndex);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isNull(int columnIndex) {
        try {
            return getRowAtCurrentPosition().isNull(columnIndex);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getColumnIndex(String columnName) {
        try {
            String[] names = getColumnNames();
            for (int i = 0; i < names.length; i++) {
                if (columnName.equals(names[i])) {
                    return i;
                }
            }
        } catch (Exception e) {
            return -1;
        }
        return -1;
    }

    @Override
    public int getColumnIndexOrThrow(String columnName) throws IllegalArgumentException {
        try {
            String[] names = getColumnNames();
            for (int i = 0; i < names.length; i++) {
                if (columnName.equals(names[i])) {
                    return i;
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
        throw new IllegalArgumentException(String.format("Could not find column '%s'", columnName));
    }

    @Override
    public String getColumnName(int columnIndex) {
        return getColumnNamesInternal()[columnIndex];
    }

    @Override
    public String[] getColumnNames() {
        return getColumnNamesInternal();
    }

    private String[] getColumnNamesInternal() {
        if (columnMapping == null) {
            columnMapping = backend.getColumnNames(query);
            if (columnMapping == null) {
                throw new IllegalStateException("unable to obtain column mapping");
            }
        }

        return columnMapping;
    }

    protected abstract JSONArray getRowAtCurrentPosition() throws JSONException;

    protected JSONArray fullQuery(String query, Object[] bindArgs) {
        return backend.fullQuery(query, bindArgs);
    }

    @Override
    public int getType(int columnIndex) {
        throw new NotImplementedException();
    }
}
