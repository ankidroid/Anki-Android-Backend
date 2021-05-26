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

import android.content.ContentResolver;
import android.database.CharArrayBuffer;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Bundle;

import timber.log.Timber;

/**
 * Base class for all database cursors, abstracting database methods to a common interface
 * Throwing on non-database-related cursor-methods
 *
 * This is useful because cursors are an android-specific implementation and not a database-specific
 * implementation, and many of the methods are not relevant.
 */
public abstract class AnkiDatabaseCursor implements Cursor {

    @Override
    public boolean isFirst() {
        return getPosition() == 0;
    }

    @Override
    public boolean isBeforeFirst() {
        return getPosition() < 0;
    }

    @Override
    public abstract int getCount();
    @Override
    public abstract int getPosition();

    @Override
    public abstract int getColumnIndex(String columnName);

    @Override
    public abstract int getColumnIndexOrThrow(String columnName) throws IllegalArgumentException;

    @Override
    public abstract String getColumnName(int columnIndex);

    @Override
    public abstract String[] getColumnNames();

    @Override
    public abstract int getColumnCount();

    @Override
    public abstract String getString(int columnIndex);

    @Override
    public abstract short getShort(int columnIndex);

    @Override
    public abstract int getInt(int columnIndex);

    @Override
    public abstract  long getLong(int columnIndex);

    @Override
    public abstract float getFloat(int columnIndex);

    @Override
    public abstract double getDouble(int columnIndex);

    @Override
    public abstract boolean isNull(int columnIndex);

    @Override
    public abstract void close();

    @Override
    public abstract boolean isClosed();

    @Override
    public abstract int getType(int columnIndex);

    @Override
    public byte[] getBlob(int columnIndex) {
        throw new NotImplementedException();
    }

    @Override
    public void setNotificationUri(ContentResolver cr, Uri uri) {
        throw new NotImplementedException();
    }

    @Override
    public void deactivate() {
        Timber.w("deactivate - not implemented - throwing");
        throw new NotImplementedException();
    }

    @Override
    public void copyStringToBuffer(int columnIndex, CharArrayBuffer buffer) {
        throw new NotImplementedException();
    }

    @Override
    public boolean requery() {
        Timber.w("requery - not implemented - throwing");
        throw new NotImplementedException();
    }

    @Override
    public Uri getNotificationUri() {
        throw new NotImplementedException();
    }

    @Override
    public boolean getWantsAllOnMoveCalls() {
        return false;
    }

    @Override
    public void setExtras(Bundle extras) {
        throw new NotImplementedException();
    }

    @Override
    public Bundle getExtras() {
        throw new NotImplementedException();
    }

    @Override
    public Bundle respond(Bundle extras) {
        throw new NotImplementedException();
    }

    @Override
    public abstract boolean moveToPosition(int position);

    @Override
    public void registerContentObserver(ContentObserver observer) {
        Timber.w("Not implemented: registerContentObserver - shouldn't matter unless requery() is called");
    }

    @Override
    public void unregisterContentObserver(ContentObserver observer) {
        Timber.w("Not implemented: unregisterContentObserver - shouldn't matter unless requery() is called");
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
        Timber.w("Not implemented: registerDataSetObserver - shouldn't matter unless requery() is called");
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
        Timber.w("Not implemented: unregisterDataSetObserver - shouldn't matter unless requery() is called");
    }


    @Override
    public boolean isLast() {
        return getPosition() == getLastPosition();
    }

    @Override
    public boolean isAfterLast() {
        return getPosition() >= getCount();
    }

    @Override
    public boolean move(int offset) {
        return moveToPosition(getPosition() + offset);
    }

    @Override
    public boolean moveToLast() {
        int toMoveTo = getLastPosition();
        return moveToPosition(toMoveTo);
    }

    @Override
    public boolean moveToFirst() {
        return moveToPosition(0);
    }

    @Override
    public boolean moveToNext() {
        int toMoveTo = getPosition() + 1;
        return moveToPosition(toMoveTo);
    }

    @Override
    public boolean moveToPrevious() {
        int toMoveTo = getPosition() - 1;
        return moveToPosition(toMoveTo);
    }

    protected int getLastPosition() {
        return getCount() - 1;
    }
}
