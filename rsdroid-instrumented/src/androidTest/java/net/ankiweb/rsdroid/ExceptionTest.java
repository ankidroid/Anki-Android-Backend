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

package net.ankiweb.rsdroid;

import net.ankiweb.rsdroid.database.NotImplementedException;
import net.ankiweb.rsdroid.exceptions.BackendDeckIsFilteredException;
import net.ankiweb.rsdroid.exceptions.BackendExistingException;
import net.ankiweb.rsdroid.exceptions.BackendInterruptedException;
import net.ankiweb.rsdroid.exceptions.BackendInvalidInputException;
import net.ankiweb.rsdroid.exceptions.BackendIoException;
import net.ankiweb.rsdroid.exceptions.BackendJsonException;
import net.ankiweb.rsdroid.exceptions.BackendNetworkException;
import net.ankiweb.rsdroid.exceptions.BackendNotFoundException;
import net.ankiweb.rsdroid.exceptions.BackendProtoException;
import net.ankiweb.rsdroid.exceptions.BackendSyncException;
import net.ankiweb.rsdroid.exceptions.BackendTemplateException;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;

import static org.junit.Assert.fail;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

@RunWith(Parameterized.class)
public class ExceptionTest {

    private BackendForTesting backend;

    @Parameterized.Parameter()
    public BackendForTesting.ErrorType errorType;

    @Parameterized.Parameter(value = 1)
    public Class<? extends Exception> clazz;

    @Parameterized.Parameters(name = "{0}")
    public static java.util.Collection<Object[]> initParameters() {
        // This does one run with schedVersion injected as 1, and one run as 2

        // If an exception does not provide enough information to handle it
        Class<NotImplementedException> NOT_POSSIBLE = NotImplementedException.class;

        return Arrays.asList(new Object[][] {
            // converted to invalid input protobuf - not available
            { BackendForTesting.ErrorType.CollectionAlreadyOpen, BackendInvalidInputException.BackendCollectionAlreadyOpenException.class },
            { BackendForTesting.ErrorType.CollectionNotOpen, BackendInvalidInputException.BackendCollectionNotOpenException.class },
            { BackendForTesting.ErrorType.SearchError, NOT_POSSIBLE },

            { BackendForTesting.ErrorType.SyncErrorAuthFailed, BackendSyncException.BackendSyncAuthFailedException.class },
            { BackendForTesting.ErrorType.SyncErrorOther, BackendSyncException.class },

            { BackendForTesting.ErrorType.DbErrorCorrupt, NOT_POSSIBLE },

            { BackendForTesting.ErrorType.DbErrorFileTooNew, BackendException.BackendDbException.BackendDbFileTooNewException.class},
            { BackendForTesting.ErrorType.DbErrorFileTooOld, BackendException.BackendDbException.BackendDbFileTooOldException.class},
            { BackendForTesting.ErrorType.DbErrorLocked, BackendException.BackendDbException.BackendDbLockedException.class},
            { BackendForTesting.ErrorType.DbErrorMissingEntity, BackendException.BackendDbException.BackendDbMissingEntityException.class},
            { BackendForTesting.ErrorType.DbErrorOther, BackendException.BackendDbException.class},
            { BackendForTesting.ErrorType.NetworkError, BackendNetworkException.class},
            { BackendForTesting.ErrorType.FilteredDeckError, BackendDeckIsFilteredException.class },
            { BackendForTesting.ErrorType.Existing, BackendExistingException.class },
            { BackendForTesting.ErrorType.Interrupted, BackendInterruptedException.class},
            { BackendForTesting.ErrorType.InvalidInput, BackendInvalidInputException.class},
            { BackendForTesting.ErrorType.JSONError, BackendJsonException.class },
            { BackendForTesting.ErrorType.ProtoError, BackendProtoException.class },
            { BackendForTesting.ErrorType.TemplateError, BackendTemplateException.class},

            { BackendForTesting.ErrorType.NotFound, BackendNotFoundException.class},
        });
    }


    @Before
    public void errorProducesNamedException() {
        backend = BackendForTesting.create();
    }

    protected Context getContext() {
        return InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    @Test
    public void testError() {
        if (NotImplementedException.class.equals(clazz)) {
            //noinspection ConstantConditions
            Assume.assumeTrue("This case cannot be handled yet", false);
        }

        assertThrows(errorType, clazz);
    }

    private void assertThrows(BackendForTesting.ErrorType e, Class<? extends Exception> clazz) {
        try {
            backend.debugProduceError(e);
            fail();
        } catch (Throwable ex) { // we catch BackendFatalError here
            if (!ex.getClass().equals(clazz)) {
                Assert.fail("ex was not an instance of " + clazz.getSimpleName() + ". Instead: " + ex.getClass() + ". message: " + ex.getMessage());
            }
        }
    }
}
