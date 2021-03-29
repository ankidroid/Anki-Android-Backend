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

/**
 * A lot of exceptions get converted to Invalid Input when returned:
 *
 * CollectionNotOpen
 * CollectionAlreadyOpen
 * SearchError
 */
public class BackendInvalidInputException extends BackendException {
    public BackendInvalidInputException(Backend.BackendError error) {
        super(error);
    }

    public static BackendInvalidInputException fromInvalidInputError(Backend.BackendError error) {
        switch (error.getLocalized()) {
            case "CollectionAlreadyOpen": return new BackendCollectionAlreadyOpenException(error);
            case "CollectionNotOpen": return new BackendCollectionNotOpenException(error);
            // TODO: We can't handle this case as there's no available properties.
            // case "SearchError": return new BackendSearchException(error);
        }
        return new BackendInvalidInputException(error);
    }

    public static class BackendCollectionAlreadyOpenException extends BackendInvalidInputException {
        public BackendCollectionAlreadyOpenException(Backend.BackendError error) {
            super(error);
        }
    }

    public static class BackendCollectionNotOpenException extends BackendInvalidInputException {
        public BackendCollectionNotOpenException(Backend.BackendError error) {
            super(error);
        }
    }

    public static class BackendSearchException extends BackendInvalidInputException {
        public BackendSearchException(Backend.BackendError error) {
            super(error);
        }
    }
}
