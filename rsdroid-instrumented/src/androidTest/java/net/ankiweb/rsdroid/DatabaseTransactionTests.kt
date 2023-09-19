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

import net.ankiweb.rsdroid.ankiutil.DatabaseUtil.queryScalar
import net.ankiweb.rsdroid.database.testutils.DatabaseComparison
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class DatabaseTransactionTests : DatabaseComparison() {
    @Test
    fun nestedTransactionFailureInside() {
        mDatabase.beginTransaction()
        mDatabase.beginTransaction()
        insert(1)
        // mDatabase.setTransactionSuccessful();
        mDatabase.endTransaction()
        insert(2)
        mDatabase.setTransactionSuccessful()
        mDatabase.endTransaction()
        assertThat(countNums(), Is.`is`(0))
    }

    @Test
    fun nestedTransactionFailureOutside() {
        mDatabase.beginTransaction()
        mDatabase.beginTransaction()
        insert(1)
        mDatabase.setTransactionSuccessful()
        mDatabase.endTransaction()
        insert(2)
        // mDatabase.setTransactionSuccessful();
        mDatabase.endTransaction()
        assertThat("count should be rolled back", countNums(), Is.`is`(0))
    }

    @Test
    fun nestedTransactionSuccess() {
        mDatabase.beginTransaction()
        mDatabase.beginTransaction()
        insert(1)
        mDatabase.setTransactionSuccessful()
        mDatabase.endTransaction()
        insert(2)
        mDatabase.setTransactionSuccessful()
        mDatabase.endTransaction()
        assertThat("count should be updated", countNums(), Is.`is`(2))
    }

    private fun insert(rowNumber: Int) {
        val s = String.format("insert into nums (id) values (%s)", rowNumber)
        mDatabase.execSQL(s)
    }

    private fun countNums(): Int {
        return queryScalar(mDatabase, "select count(*) from nums")
    }
}