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

package net.ankiweb.rsdroid.database;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import static net.ankiweb.rsdroid.database.RustSupportSQLiteDatabase.shouldUseLimitOffsetQuery;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class RustSupportSQLiteDatabaseTests {

    @Test
    @Ignore("Requires #14")
    public void limitOffsetTests() {
        assertTrue(shouldUseLimitOffsetQuery("select * from cards"));
        assertFalse(shouldUseLimitOffsetQuery("select * from cards limit 1"));
        assertFalse(shouldUseLimitOffsetQuery("insert into cards (id) VALUES (1)"));
    }

    @Ignore("Not implemented")
    @Test
    public void limitCte() {
        assertTrue(shouldUseLimitOffsetQuery("select count() from (select * from cards limit 1)"));
    }


    @Ignore("Not implemented")
    @Test
    public void selectWithSpacing() {
        assertTrue(shouldUseLimitOffsetQuery("  select count() from (select * from cards limit 1)"));
    }
}
