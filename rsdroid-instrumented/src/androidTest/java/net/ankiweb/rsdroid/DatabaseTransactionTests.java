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

import net.ankiweb.rsdroid.ankiutil.DatabaseUtil;
import net.ankiweb.rsdroid.database.testutils.DatabaseComparison;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@RunWith(Parameterized.class)
public class DatabaseTransactionTests extends DatabaseComparison {

    @Test
    public void nestedTransactionFailureInside() {
        mDatabase.beginTransaction();
        mDatabase.beginTransaction();
        insert(1);
        // mDatabase.setTransactionSuccessful();
        mDatabase.endTransaction();
        insert(2);
        mDatabase.setTransactionSuccessful();
        mDatabase.endTransaction();

        assertThat(countNums(), is(0));
    }


    @Test
    public void nestedTransactionFailureOutside() {
        mDatabase.beginTransaction();
        mDatabase.beginTransaction();
        insert(1);
        mDatabase.setTransactionSuccessful();
        mDatabase.endTransaction();
        insert(2);
        // mDatabase.setTransactionSuccessful();
        mDatabase.endTransaction();

        assertThat("count should be rolled back", countNums(), is(0));
    }

    @Test
    public void nestedTransactionSuccess() {
        mDatabase.beginTransaction();
        mDatabase.beginTransaction();
        insert(1);
        mDatabase.setTransactionSuccessful();
        mDatabase.endTransaction();
        insert(2);
        mDatabase.setTransactionSuccessful();
        mDatabase.endTransaction();

        assertThat("count should be updated", countNums(), is(2));
    }

    private void insert(int rowNumber) {
        String s = String.format("insert into nums (id) values (%s)", rowNumber);
        mDatabase.execSQL(s);
    }

    private int countNums() {
        return DatabaseUtil.queryScalar(mDatabase, "select count(*) from nums");
    }
}
