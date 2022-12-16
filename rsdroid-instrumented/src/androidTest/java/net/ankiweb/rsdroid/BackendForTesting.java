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

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.util.Arrays;

public class BackendForTesting extends Backend {

    public BackendForTesting(@NonNull Context context, @NonNull Iterable<String> langs, boolean legacySchema) {
        super(context, langs, legacySchema);
    }

    public static BackendForTesting create(@NonNull Context context) {
        return new BackendForTesting(context, Arrays.asList("en"), true);
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
        DbErrorFileTooNew,
        DbErrorFileTooOld,
        DbErrorMissingEntity,
        DbErrorCorrupt,
        DbErrorLocked,
        DbErrorOther,
        NetworkError,
        SyncErrorAuthFailed,
        SyncErrorOther,
        JSONError,
        ProtoError,
        Interrupted,
        CollectionNotOpen,
        CollectionAlreadyOpen,
        NotFound,
        Existing,
        FilteredDeckError,
        SearchError,
        FatalError;
    }
}
