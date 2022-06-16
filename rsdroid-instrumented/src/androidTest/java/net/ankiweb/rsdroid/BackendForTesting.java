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

package net.ankiweb.rsdroid;

import androidx.annotation.VisibleForTesting;

import com.google.protobuf.InvalidProtocolBufferException;

import anki.backend;

public class BackendForTesting extends BackendV1Impl {

    BackendForTesting() {
        super();
    }

    public static BackendForTesting create() {
        try {
            NativeMethods.ensureSetup();
        } catch (RustBackendFailedException e) {
            throw new RuntimeException(e);
        }
        return new BackendForTesting();
    }


    // Debug methods

    /**
     * Throws a given error, generated from the Rust
     * @param error The error to throw
     */
    @VisibleForTesting
    public void debugProduceError(ErrorType error) {
        super.debugProduceError(error.toString());
        throw new IllegalStateException("An exception should have been thrown");
    }


    public enum ErrorType {
        InvalidInput,
        TemplateError,
        TemplateSaveError,
        IOError,
        DbErrorFileTooNew,
        DbErrorFileTooOld,
        DbErrorMissingEntity,
        DbErrorCorrupt,
        DbErrorLocked,
        DbErrorOther,
        NetworkErrorOffline,
        NetworkErrorTimeout,
        NetworkErrorProxyAuth,
        NetworkErrorOther,
        SyncErrorConflict,
        SyncErrorServerError,
        SyncErrorClientTooOld,
        SyncErrorAuthFailed,
        SyncErrorServerMessage,
        SyncErrorClockIncorrect,
        SyncErrorOther,
        SyncErrorResyncRequired,
        SyncErrorDatabaseCheckRequired,
        JSONError,
        ProtoError,
        Interrupted,
        CollectionNotOpen,
        CollectionAlreadyOpen,
        NotFound,
        Existing,
        DeckIsFiltered,
        SearchError,
        FatalError;

        public String toString() {
            switch (this) {
                case InvalidInput:
                    return "InvalidInput";
                case TemplateError:
                    return "TemplateError";
                case TemplateSaveError:
                    return "TemplateSaveError";
                case IOError:
                    return "IOError";
                case DbErrorFileTooNew:
                    return "DbErrorFileTooNew";
                case DbErrorFileTooOld:
                    return "DbErrorFileTooOld";
                case DbErrorMissingEntity:
                    return "DbErrorMissingEntity";
                case DbErrorCorrupt:
                    return "DbErrorCorrupt";
                case DbErrorLocked:
                    return "DbErrorLocked";
                case DbErrorOther:
                    return "DbErrorOther";
                case NetworkErrorOffline:
                    return "NetworkErrorOffline";
                case NetworkErrorTimeout:
                    return "NetworkErrorTimeout";
                case NetworkErrorProxyAuth:
                    return "NetworkErrorProxyAuth";
                case NetworkErrorOther:
                    return "NetworkErrorOther";
                case SyncErrorConflict:
                    return "SyncErrorConflict";
                case SyncErrorServerError:
                    return "SyncErrorServerError";
                case SyncErrorClientTooOld:
                    return "SyncErrorClientTooOld";
                case SyncErrorAuthFailed:
                    return "SyncErrorAuthFailed";
                case SyncErrorServerMessage:
                    return "SyncErrorServerMessage";
                case SyncErrorClockIncorrect:
                    return "SyncErrorClockIncorrect";
                case SyncErrorOther:
                    return "SyncErrorOther";
                case SyncErrorResyncRequired:
                    return "SyncErrorResyncRequired";
                case SyncErrorDatabaseCheckRequired:
                    return "SyncErrorDatabaseCheckRequired";
                case JSONError:
                    return "JSONError";
                case ProtoError:
                    return "ProtoError";
                case Interrupted:
                    return "Interrupted";
                case CollectionNotOpen:
                    return "CollectionNotOpen";
                case CollectionAlreadyOpen:
                    return "CollectionAlreadyOpen";
                case NotFound:
                    return "NotFound";
                case Existing:
                    return "Existing";
                case DeckIsFiltered:
                    return "DeckIsFiltered";
                case SearchError:
                    return "SearchError";
                case FatalError:
                    return "FatalError";
                default: throw new IllegalStateException("Unknown: " + this);
            }
        }
    }
}
