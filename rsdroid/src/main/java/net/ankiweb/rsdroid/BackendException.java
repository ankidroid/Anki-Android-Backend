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

import BackendProto.Backend;

public class BackendException extends RuntimeException {
    private final Backend.BackendError mError;

    public BackendException(Backend.BackendError error)  {
        super(error.getLocalized());
        this.mError = error;
    }

    public static BackendException fromError(Backend.BackendError error) {
        return new BackendException(error);
    }

    public static RuntimeException fromException(Exception ex) {
        return new RuntimeException(ex);
    }
}
