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

package net.ankiweb.rsdroid.database;

import android.database.sqlite.SQLiteDatabaseCorruptException;

import net.ankiweb.rsdroid.BackendException;
import net.ankiweb.rsdroid.database.testutils.DatabaseCorruption;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.typeCompatibleWith;

@RunWith(Parameterized.class)
public class DatabaseRegularCorruptionTest extends DatabaseCorruption {
    // In both cases, openCollection fails with the exception.

    @Override
    protected void assertCorruption(Exception setupException) {
        // Rust: net.ankiweb.rsdroid.BackendException$BackendDbException: DBError { info: "SqliteFailure(Error { code: DatabaseCorrupt, extended_code: 11 }, Some(\"database disk image is malformed\"))", kind: Other }
        // Java: database disk image is malformed (code 11): , while compiling: PRAGMA journal_mode

//        assertThat(setupException.getClass(), typeCompatibleWith(BackendException.BackendDbException.class));
        assertThat(setupException.getClass(), typeCompatibleWith(SQLiteDatabaseCorruptException.class));

        // this mapping to an unrelated exception should be done at a higher level

        assertThat(setupException.getLocalizedMessage(), containsString("database disk image is malformed"));
        assertThat(setupException.getLocalizedMessage(), containsString("11"));
    }

    @Override
    protected String getCorruptDatabaseAssetName() {
        return "initial_version_2_12_1_corrupt_regular.anki2";
    }
}