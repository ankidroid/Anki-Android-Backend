/*
 * Copyright (c) 2021 David Allison <davidallisongithub@gmail.com>
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


import android.database.CursorIndexOutOfBoundsException;
import android.database.sqlite.SQLiteException;

import net.ankiweb.rsdroid.BackendException;

import java.util.Locale;

import BackendProto.Sqlite;

public class StreamingProtobufSQLiteCursor extends AnkiDatabaseCursor {
    /**
     * Rust Implementation:
     *
     * When we request a query, rust calculates 2MB (default) of results and sends it to us
     *
     * We keep track of where we are with getSliceStartIndex: the index into the rust collection
     *
     * The next request should be for index: getSliceStartIndex() + getCurrentSliceRowCount()
     */

    private final SQLHandler backend;
    private final String query;
    private Sqlite.DBResponse results;
    private int position = -1;
    private String[] columnMapping;
    private boolean isClosed = false;
    private final int sequenceNumber;
    /** The total number of rows for the query */
    private final int rowCount;

    /**The current index into the collection or rows */
    private long getSliceStartIndex() {
        return results.getStartIndex();
    }

    public StreamingProtobufSQLiteCursor(SQLHandler backend, String query, Object[] bindArgs) {
        this.backend = backend;
        this.query = query;

        try {
            results = this.backend.fullQueryProto(this.query, bindArgs);
            sequenceNumber = results.getSequenceNumber();
            rowCount = results.getRowCount();
        } catch (BackendException e) {
            throw e.toSQLiteException(this.query);
        }
    }

    private void getNextPage() {
        position = -1;

        try {
            results = backend.getNextSlice(getSliceStartIndex() + getCurrentSliceRowCount(), sequenceNumber);
            if (results.getSequenceNumber() != sequenceNumber) {
                throw new IllegalStateException("rsdroid does not currently handle nested cursor-based queries. Please change the code to avoid holding a reference to the query, or implement the functionality in rsdroid");
            }
        } catch (BackendException e) {
            throw e.toSQLiteException(query);
        }
    }

    @Override
    public int getCount() {
        return rowCount;
    }

    @Override
    public int getPosition() {
        return (int) getSliceStartIndex() + position;
    }

    @Override
    public boolean moveToFirst() {
        if (getCurrentSliceRowCount() == 0) {
            return false;
        }
        position = 0;
        return true;
    }

    @Override
    public boolean moveToNext() {
        if (getCurrentSliceRowCount() > 0 && position + 1 >= getCurrentSliceRowCount() && getCount() != getCurrentSliceRowCount()) {
            getNextPage();
        }
        position++;
        return getCurrentSliceRowCount() != 0 && position < getCurrentSliceRowCount();
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

    @Override
    public int getColumnCount() {
        if (getCurrentSliceRowCount() == 0) {
            return 0;
        } else {
            return results.getResult().getRows(0).getFieldsCount();
        }
    }

    @Override
    public String getString(int columnIndex) {
        Sqlite.SqlValue field = getFieldAtIndex(columnIndex);
        switch (field.getDataCase()) {
            case BLOBVALUE: throw new SQLiteException("unknown error (code 0): Unable to convert BLOB to string");
            case LONGVALUE: return Long.toString(field.getLongValue());
            case DOUBLEVALUE: return Double.toString(field.getDoubleValue());
            case STRINGVALUE: return field.getStringValue();
            case DATA_NOT_SET: return null;
            default: throw new IllegalStateException("Unknown data case: " + field.getDataCase());
        }
    }

    @SuppressWarnings("DuplicateBranchesInSwitch")
    @Override
    public long getLong(int columnIndex) {
        Sqlite.SqlValue field = getFieldAtIndex(columnIndex);
        switch (field.getDataCase()) {
            case BLOBVALUE: throw new SQLiteException("unknown error (code 0): Unable to convert BLOB to long");
            case LONGVALUE: return field.getLongValue();
            case DOUBLEVALUE: return (long) field.getDoubleValue();
            case STRINGVALUE: return 0;
            case DATA_NOT_SET: return 0;
            default: throw new IllegalStateException("Unknown data case: " + field.getDataCase());
        }
    }

    @SuppressWarnings("DuplicateBranchesInSwitch")
    @Override
    public double getDouble(int columnIndex) {
        Sqlite.SqlValue field = getFieldAtIndex(columnIndex);
        switch (field.getDataCase()) {
            case BLOBVALUE: throw new SQLiteException("unknown error (code 0): Unable to convert BLOB to double");
            case LONGVALUE: return field.getLongValue();
            case DOUBLEVALUE: return field.getDoubleValue();
            case STRINGVALUE: return 0d;
            case DATA_NOT_SET: return 0d;
            default: throw new IllegalStateException("Unknown data case: " + field.getDataCase());
        }
    }

    @Override
    public short getShort(int columnIndex) {
        return (short) getLong(columnIndex);
    }

    @Override
    public int getInt(int columnIndex) {
        return (int) getLong(columnIndex);
    }

    @Override
    public float getFloat(int columnIndex) {
        return (float) getDouble(columnIndex);
    }

    private boolean isNull(Sqlite.SqlValue field) {
        return field.getDataCase() == Sqlite.SqlValue.DataCase.DATA_NOT_SET;
    }

    @Override
    public boolean isNull(int columnIndex) {
        Sqlite.SqlValue field = getFieldAtIndex(columnIndex);
        return isNull(field);
    }

    @Override
    public void close() {
        isClosed = true;
        backend.cancelCurrentProtoQuery(sequenceNumber);
    }

    @Override
    public boolean isClosed() {
        return isClosed;
    }

    protected Sqlite.Row getRowAtCurrentPosition() {
        Sqlite.DBResult result = results.getResult();
        int rowCount = getCurrentSliceRowCount();
        if (position < 0 || position >= rowCount) {
            throw new CursorIndexOutOfBoundsException(String.format(Locale.ROOT, "Index %d requested, with a size of %d", position, rowCount));
        }
        return result.getRows(position);
    }

    private Sqlite.SqlValue getFieldAtIndex(int columnIndex) {
        return getRowAtCurrentPosition().getFields(columnIndex);
    }

    protected int getCurrentSliceRowCount() {
        return results.getResult().getRowsCount();
    }
}

