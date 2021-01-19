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
    // Interleaved cursors would corrupt data if there are more than PAGE_SIZE results.
    // We currently use mSequenceNumber to crash if this is the case

    // MAINTENANCE: This is not obtained from the Rust, so must manually be kept in sync
    public static final int RUST_PAGE_SIZE = 1000;

    private final SQLHandler mBackend;
    private final String mQuery;
    private Sqlite.DBResponse mResults;
    private int mPosition = -1;
    private int mPage = -1;
    private String[] mColumnMapping;
    private boolean mClosed = false;
    private final int mSequenceNumber;


    public StreamingProtobufSQLiteCursor(SQLHandler backend, String query, Object[] bindArgs) {
        this.mBackend = backend;
        this.mQuery = query;

        mPage++;
        try {
            mResults = mBackend.fullQueryProto(mQuery, bindArgs);
            mSequenceNumber = mResults.getSequenceNumber();
        } catch (BackendException e) {
            throw e.toSQLiteException(mQuery);
        }
    }

    private void getNextPage() {
        mPage++;
        mPosition = -1;

        try {
            mResults = mBackend.getPage(mPage);
            if (mResults.getSequenceNumber() != mSequenceNumber) {
                throw new IllegalStateException("rsdroid does not currently handle nested cursor-based queries. Please change the code to avoid holding a reference to the query, or implement the functionality in rsdroid");
            }
        } catch (BackendException e) {
            throw e.toSQLiteException(mQuery);
        }
    }

    @Override
    public int getCount() {
        // BUG: This will fail if we've iterated the whole collection.
        int currentRowCount = mBackend.getCurrentRowCount();
        if (currentRowCount == -1) {
            throw new IllegalStateException("Unable to obtain row count");
        }
        return currentRowCount;
    }

    @Override
    public int getPosition() {
        return mPosition;
    }

    @Override
    public boolean moveToFirst() {
        if (getCurrentSliceRowCount() == 0) {
            return false;
        }
        mPosition = 0;
        return true;
    }

    @Override
    public boolean moveToNext() {
        if (getCurrentSliceRowCount() > 0 && mPosition + 1 >= RUST_PAGE_SIZE) {
            getNextPage();
        }
        mPosition++;
        return getCurrentSliceRowCount() != 0 && mPosition < getCurrentSliceRowCount();
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
        if (mColumnMapping == null) {
            mColumnMapping = mBackend.getColumnNames(mQuery);
            if (mColumnMapping == null) {
                throw new IllegalStateException("unable to obtain column mapping");
            }
        }

        return mColumnMapping;
    }

    @Override
    public int getColumnCount() {
        if (getCurrentSliceRowCount() == 0) {
            return 0;
        } else {
            return mResults.getResult().getRows(0).getFieldsCount();
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
        mClosed = true;
        mBackend.cancelCurrentProtoQuery();
    }

    @Override
    public boolean isClosed() {
        return mClosed;
    }

    protected Sqlite.Row getRowAtCurrentPosition() {
        Sqlite.DBResult result = mResults.getResult();
        int rowCount = getCurrentSliceRowCount();
        if (mPosition < 0 || mPosition >= rowCount) {
            throw new CursorIndexOutOfBoundsException(String.format(Locale.ROOT, "Index %d requested, with a size of %d", mPosition, rowCount));
        }
        return result.getRows(mPosition);
    }

    private Sqlite.SqlValue getFieldAtIndex(int columnIndex) {
        return getRowAtCurrentPosition().getFields(columnIndex);
    }

    private int getCurrentSliceRowCount() {
        return mResults.getResult().getRowsCount();
    }
}

