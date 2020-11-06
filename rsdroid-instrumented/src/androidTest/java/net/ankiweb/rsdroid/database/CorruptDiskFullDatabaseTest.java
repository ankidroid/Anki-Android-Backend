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

import android.database.sqlite.SQLiteFullException;

import net.ankiweb.rsdroid.database.testutils.DatabaseCorruption;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.typeCompatibleWith;

/** It was (pleasantly) surprisingly hard to corrupt a database via java */
@RunWith(Parameterized.class)
public class CorruptDiskFullDatabaseTest extends DatabaseCorruption {

    @Override
    protected void assertCorruption(Exception mException) {
        // error while compiling: "create table nums (id int)": DBError { info: "SqliteFailure(Error { code: DiskFull, extended_code: 13 }, Some(\"database or disk is full\"))", kind: Other }
        assertThat(mException.getClass(), typeCompatibleWith(SQLiteFullException.class));
        // Java: "database or disk is full (code 13)"
        assertThat(mException.getLocalizedMessage(), containsString("database or disk is full"));
        assertThat(mException.getLocalizedMessage(), containsString("13"));
    }

    @Override
    protected String getCorruptDatabaseAssetName() {
        return "initial_version_2_12_1_corrupt_diskfull.anki2";
    }
}
