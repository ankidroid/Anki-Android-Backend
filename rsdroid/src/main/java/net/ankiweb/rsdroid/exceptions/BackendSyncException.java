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

package net.ankiweb.rsdroid.exceptions;

import net.ankiweb.rsdroid.BackendException;

import BackendProto.Backend;

public class BackendSyncException extends BackendException {

    public BackendSyncException(Backend.BackendError error) {
        super(error);
    }


    public static BackendSyncException fromSyncError(Backend.BackendError error) {

        switch (error.getSyncError().getKind()) {
            case CONFLICT:
                throw new BackendSyncConflictException(error);
            case AUTH_FAILED:
                throw new BackendSyncAuthFailedException(error);
            case SERVER_ERROR:
                throw new BackendSyncServerErrorException(error);
            case UNRECOGNIZED:
                throw new BackendSyncUnrecognizedException(error);
            case CLIENT_TOO_OLD:
                throw new BackendSyncClientTooOldException(error);
            case SERVER_MESSAGE:
                throw new BackendSyncServerMessageException(error);
            case CLOCK_INCORRECT:
                throw new BackendSyncClockIncorrectException(error);
            case RESYNC_REQUIRED:
                throw new BackendSyncResyncRequiredException(error);
            case MEDIA_CHECK_REQUIRED:
                throw new BackendSyncMediaCheckRequiredException(error);
            case DATABASE_CHECK_REQUIRED:
                throw new BackendSyncDatabaseCheckRequiredException(error);
            case OTHER:
            default:
                throw new BackendSyncException(error);
        }
    }

    public static class BackendSyncConflictException extends BackendSyncException {
        public BackendSyncConflictException(Backend.BackendError error) {
            super(error);
        }
    }

    public static class BackendSyncAuthFailedException extends BackendSyncException {
        public BackendSyncAuthFailedException(Backend.BackendError error) {
            super(error);
        }
    }

    public static class BackendSyncServerErrorException extends BackendSyncException {
        public BackendSyncServerErrorException(Backend.BackendError error) {
            super(error);
        }
    }

    public static class BackendSyncUnrecognizedException extends BackendSyncException {
        public BackendSyncUnrecognizedException(Backend.BackendError error) {
            super(error);
        }
    }

    public static class BackendSyncClientTooOldException extends BackendSyncException {
        public BackendSyncClientTooOldException(Backend.BackendError error) {
            super(error);
        }
    }

    public static class BackendSyncClockIncorrectException extends BackendSyncException {
        public BackendSyncClockIncorrectException(Backend.BackendError error) {
            super(error);
        }
    }

    public static class BackendSyncServerMessageException extends BackendSyncException {
        public BackendSyncServerMessageException(Backend.BackendError error) {
            super(error);
        }
    }

    public static class BackendSyncResyncRequiredException extends BackendSyncException {
        public BackendSyncResyncRequiredException(Backend.BackendError error) {
            super(error);
        }
    }

    public static class BackendSyncMediaCheckRequiredException extends BackendSyncException {
        public BackendSyncMediaCheckRequiredException(Backend.BackendError error) {
            super(error);
        }
    }

    public static class BackendSyncDatabaseCheckRequiredException extends BackendSyncException {
        public BackendSyncDatabaseCheckRequiredException(Backend.BackendError error) {
            super(error);
        }
    }
}
