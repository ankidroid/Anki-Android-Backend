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

/** A Java implementation of a rust "panic".
 *
 * rsdroid-bridge is not yet unwind-safe. If a panic occurs, we may be in an incoherent state
 *
 * But, we don't want the rust to panic. This causes a native exception, which will kill AnkiDroid
 * before ACRA can send an exception report to the crash reporting server.
 *
 * This error delays the panic in a form that ACRA can catch, log, then crash more gracefully with.
 */
public class BackendFatalError extends Error {
    public BackendFatalError(String message) {
        super(message);
    }
}
