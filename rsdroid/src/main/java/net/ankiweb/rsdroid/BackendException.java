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

package net.ankiweb.rsdroid;

import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabaseCorruptException;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteFullException;

import java.util.Locale;

import BackendProto.Backend;

public class BackendException extends RuntimeException {
    private final Backend.BackendError mError;

    public BackendException(Backend.BackendError error)  {
        super(error.getLocalized());
        this.mError = error;
    }

    public static BackendException fromError(Backend.BackendError error) {
        if (error.hasDbError()) {
            return new BackendDbException(error);
        }

        return new BackendException(error);
    }

    public static RuntimeException fromException(Exception ex) {
        return new RuntimeException(ex);
    }


    public RuntimeException toSQLiteException(String query) {
        String message = String.format(Locale.ROOT, "error while compiling: \"%s\": %s", query, this.getLocalizedMessage());
        return new SQLiteException(message, this);
    }

    public static class BackendDbException extends BackendException {

        public BackendDbException(Backend.BackendError error) {
            // This is very simple for now and matches Anki Desktop (error is currently text)
            // Later on, we may want to use structured error messages
            // DBError { info: "SqliteFailure(Error { code: Unknown, extended_code: 1 }, Some(\"no such table: aa\"))", kind: Other }
            super(error);
        }

        @Override
        public RuntimeException toSQLiteException(String query) {
            String message = this.getLocalizedMessage();

            if (message.contains("ConstraintViolation")) {
                throw new SQLiteConstraintException(message);
            } else if (message.contains("DiskFull")) {
                throw new SQLiteFullException(message);
            } else if (message.contains("DatabaseCorrupt")) {
                String outMessage = String.format(Locale.ROOT, "error while compiling: \"%s\": %s", query, getLocalizedMessage());
                throw new SQLiteDatabaseCorruptException(outMessage);
            } else {
                String outMessage = String.format(Locale.ROOT, "error while compiling: \"%s\": %s", query, getLocalizedMessage());
                throw new SQLiteException(outMessage, this);
            }
        }
    }
}
