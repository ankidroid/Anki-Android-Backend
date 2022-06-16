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

import androidx.annotation.Nullable;

import net.ankiweb.rsdroid.database.NotImplementedException;
import net.ankiweb.rsdroid.exceptions.BackendDeckIsFilteredException;
import net.ankiweb.rsdroid.exceptions.BackendExistingException;
import net.ankiweb.rsdroid.exceptions.BackendInterruptedException;
import net.ankiweb.rsdroid.exceptions.BackendInvalidInputException;
import net.ankiweb.rsdroid.exceptions.BackendIoException;
import net.ankiweb.rsdroid.exceptions.BackendJsonException;
import net.ankiweb.rsdroid.exceptions.BackendNetworkException;
import net.ankiweb.rsdroid.exceptions.BackendNotFoundException;
import net.ankiweb.rsdroid.exceptions.BackendProtoException;
import net.ankiweb.rsdroid.exceptions.BackendSyncException;
import net.ankiweb.rsdroid.exceptions.BackendTemplateException;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import anki.backend.BackendError.Kind;

public class BackendException extends RuntimeException {
    @SuppressWarnings({"unused", "RedundantSuppression"})
    @Nullable
    private final anki.backend.BackendError error;

    public BackendException(anki.backend.BackendError error)  {
        super(error.getLocalized());
        this.error = error;
    }

    public BackendException(String message) {
        super(message);
        error = null;
    }

    public static BackendException fromError(anki.backend.BackendError error) {
        throw new NotImplementedException();
//        switch (error.getKindValue()) {
//            case Kind.DB_ERROR.:
//                return BackendDbException.fromDbError(error);
////            case Kind.JSON_ERROR:
////                return new BackendJsonException(error.getJsonError());
//            case Kind.SYNC_AUTH_ERROR:
//                return new BackendSyncException.BackendSyncAuthFailedException(error);
//            case Kind.SYNC_OTHER_ERROR:
//                return new BackendSyncException.BackendSyncException(error);
//                return BackendSyncException.fromSyncError(error);
//            case Kind.FATAL_ERROR:
//                // This should have produced a hasFatalError property
//                throw new BackendFatalError(error.getFatalError());
//            case Kind.EXISTS:
//                return new BackendExistingException(error);
////            case Kind.DECK_IS_FILTERED:
////                return new BackendDeckIsFilteredException(error);
//            case Kind.INTERRUPTED:
//                return new BackendInterruptedException(error);
//            case Kind.PROTO_ERROR:
//                return new BackendProtoException(error);
//            case Kind.NOT_FOUND_ERROR:
//                return new BackendNotFoundException(error);
//            case Kind.INVALID_INPUT:
//                return BackendInvalidInputException.fromInvalidInputError(error);
//            case Kind.NETWORK_ERROR:
//                return BackendNetworkException.fromNetworkError(error);
//            case Kind.TEMPLATE_PARSE:
//                return BackendTemplateException.fromTemplateError(error);
//            case Kind.IO_ERROR:
//                return new BackendIoException(error);
//            case Kind.VALUE_NOT_SET:
//        }


//        return new BackendException(error);
    }

    public static RuntimeException fromException(Exception ex) {
        return new RuntimeException(ex);
    }


    public RuntimeException toSQLiteException(String query) {
        String message = String.format(Locale.ROOT, "error while compiling: \"%s\": %s", query, this.getLocalizedMessage());
        return new SQLiteException(message, this);
    }

    public static class BackendDbException extends BackendException {

        public BackendDbException(anki.backend.BackendError error) {
            // This is very simple for now and matches Anki Desktop (error is currently text)
            // Later on, we may want to use structured error messages
            // DBError { info: "SqliteFailure(Error { code: Unknown, extended_code: 1 }, Some(\"no such table: aa\"))", kind: Other }
            super(error);
        }

        public static BackendException fromDbError(anki.backend.BackendError error) {

            String localised = error.getLocalized();

            if (localised == null) {
                return new BackendDbException(error);
            }

            if (localised.contains("kind: FileTooNew")) {
                return new BackendDbFileTooNewException(error);
            }
            if (localised.contains("kind: FileTooOld")) {
                return new BackendDbFileTooOldException(error);
            }
            if (localised.contains("kind: MissingEntity")) {
                return new BackendDbMissingEntityException(error);
            }
            if (localised.contains("kind: Other")) {
                return new BackendDbException(error);
            }
            // Anki already open, or media currently syncing.
            if (localised.startsWith("Anki already open")) {
                return new BackendDbLockedException(error);
            }

            return new BackendDbException(error);
        }

        @Override
        public RuntimeException toSQLiteException(String query) {
            String message = this.getLocalizedMessage();

            if (message == null) {
                String outMessage = String.format(Locale.ROOT, "Unknown error while compiling: \"%s\"", query);
                throw new SQLiteException(outMessage, this);
            }

            if (message.contains("InvalidParameterCount")) {
                Matcher p = Pattern.compile("InvalidParameterCount\\((\\d*), (\\d*)\\)").matcher(this.getMessage());
                if (p.find()) {
                    int paramCount = Integer.parseInt(p.group(1));
                    int index = Integer.parseInt(p.group(2));
                    String errorMessage = String.format(Locale.ROOT, "Cannot bind argument at index %d because the index is out of range.  The statement has %d parameters.", index, paramCount);
                    throw new IllegalArgumentException(errorMessage, this);
                }
            } else if (message.contains("ConstraintViolation")) {
                throw new SQLiteConstraintException(message);
            } else if (message.contains("DiskFull")) {
                throw new SQLiteFullException(message);
            } else if (message.contains("DatabaseCorrupt")) {
                String outMessage = String.format(Locale.ROOT, "error while compiling: \"%s\": %s", query, message);
                throw new SQLiteDatabaseCorruptException(outMessage);
            }

            String outMessage = String.format(Locale.ROOT, "error while compiling: \"%s\": %s", query, message);
            throw new SQLiteException(outMessage, this);
        }

        public static class BackendDbFileTooNewException extends BackendException {
            public BackendDbFileTooNewException(anki.backend.BackendError error) {
                super(error);
            }
        }

        public static class BackendDbFileTooOldException extends BackendException {
            public BackendDbFileTooOldException(anki.backend.BackendError error) {
                super(error);
            }
        }

        public static class BackendDbLockedException extends BackendException {
            public BackendDbLockedException(anki.backend.BackendError error) {
                super(error);
            }
        }

        public static class BackendDbMissingEntityException extends BackendException {
            public BackendDbMissingEntityException(anki.backend.BackendError error) {
                super(error);
            }
        }
    }
}
