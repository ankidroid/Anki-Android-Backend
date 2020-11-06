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

package net.ankiweb.rsdroid.database.testutils;

import net.ankiweb.rsdroid.Shared;

import org.hamcrest.Matchers;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;

public abstract class DatabaseCorruption extends DatabaseComparison {

    private String mDatabasePath = null;
    private Exception mException;

    @Override
    protected boolean handleSetupException(Exception e) {
        this.mException = e;
        return true;
    }

    @Override
    protected String getDatabasePath() {
        if (mDatabasePath != null) {
            return mDatabasePath;
        }
        try {
            String testFilePath = Shared.getTestFilePath(getContext(), getCorruptDatabaseAssetName());
            mDatabasePath = testFilePath;
            return testFilePath;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected abstract String getCorruptDatabaseAssetName();

    @Test
    public void testCorruption() {
        assertThat(mException, Matchers.notNullValue());
        assertCorruption(mException);
    }

    protected abstract void assertCorruption(Exception mException);
}
