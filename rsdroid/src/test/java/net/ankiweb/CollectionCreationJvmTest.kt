// SPDX-License-Identifier: GPL-3.0-or-later

package net.ankiweb

import net.ankiweb.rsdroid.BackendFactory.getBackend
import net.ankiweb.rsdroid.testing.RustBackendLoader.ensureSetup
import org.junit.Assert.assertEquals
import org.junit.Test

/** Verifies collection creation through the backend API on a plain JVM. */
class CollectionCreationJvmTest {
    @Test
    fun collectionOpensAndAnswersQueries() {
        ensureSetup()
        getBackend().use { backend ->
            backend.openCollection(":memory:")
            val response = backend.fullQueryProto("SELECT count(*) FROM col", emptyArray())
            assertEquals(1, response.rowCount)
            val row = response.result.getRows(0)
            assertEquals(1L, row.getFields(0).longValue)
        }
    }
}
