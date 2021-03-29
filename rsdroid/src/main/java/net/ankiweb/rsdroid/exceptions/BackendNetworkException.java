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

public class BackendNetworkException extends BackendException {
    public BackendNetworkException(Backend.BackendError error) {
        super(error);
    }

    public static BackendNetworkException fromNetworkError(Backend.BackendError error) {

        if (!error.hasNetworkError()) {
            return new BackendNetworkException(error);
        }

        Backend.NetworkError networkError = error.getNetworkError();

        switch (networkError.getKind()) {
            case OFFLINE: return new BackendNetworkOfflineException(error);
            case TIMEOUT: return new BackendNetworkTimeoutException(error);
            case PROXY_AUTH: return new BackendNetworkProxyAuthException(error);
            case UNRECOGNIZED:
            case OTHER:
        }

        return new BackendNetworkException(error);
    }

    public static class BackendNetworkOfflineException extends BackendNetworkException {
        public BackendNetworkOfflineException(Backend.BackendError error) {
            super(error);
        }
    }

    public static class BackendNetworkTimeoutException extends BackendNetworkException {
        public BackendNetworkTimeoutException(Backend.BackendError error) {
            super(error);
        }
    }

    public static class BackendNetworkProxyAuthException extends BackendNetworkException {
        public BackendNetworkProxyAuthException(Backend.BackendError error) {
            super(error);
        }
    }
}
