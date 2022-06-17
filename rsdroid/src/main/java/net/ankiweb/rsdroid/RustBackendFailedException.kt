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
package net.ankiweb.rsdroid

@RustV1Cleanup("This exists to force implementers to handle a `rsdroid failed to load` case" +
        "as I do not trust our ~16k target devices will all export the appropriate" +
        "functions allowing for rsdroid to be loaded." +
        "This exists to ensure that there is a valid (working) fallback for V1 of the rust conversion" +
        "Once we prove this to be incorrect (or fix the bugs), we could remove this and assume that" +
        "rsdroid will always load without issue")
class RustBackendFailedException : Exception {
    constructor(error: Throwable?) : super(error)
    constructor(message: String?) : super(message)
}