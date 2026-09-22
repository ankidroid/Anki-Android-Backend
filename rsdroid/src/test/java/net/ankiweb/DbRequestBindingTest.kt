// SPDX-License-Identifier: GPL-3.0-or-later
package net.ankiweb

import androidx.test.ext.junit.runners.AndroidJUnit4
import anki.ankidroid.SqlValue.DataCase
import net.ankiweb.rsdroid.BackendFactory.getBackend
import net.ankiweb.rsdroid.testing.RustBackendLoader.ensureSetup
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DbRequestBindingTest {
    @Test
    fun bindArgumentsKeepTheirSqliteTypesAndValues() {
        ensureSetup()
        getBackend().use { backend ->
            backend.openCollection(":memory:")
            val blob = byteArrayOf(0, 1, 127)
            val text = "quote\" slash/line\n🎴"
            val result =
                backend.fullQueryProto(
                    "SELECT typeof(?1), ?1, typeof(?2), ?2, typeof(?3), ?3, " +
                        "typeof(?4), ?4, typeof(?5), ?5",
                    arrayOf(1751234567890.0, 0.1f, blob, text, null),
                )
            assertEquals(1, result.rowCount)
            val fields = result.result.getRows(0).fieldsList
            assertEquals("integer", fields[0].stringValue)
            assertEquals(1751234567890L, fields[1].longValue)
            assertEquals("real", fields[2].stringValue)
            assertEquals(0.1, fields[3].doubleValue, 0.0)
            assertEquals("blob", fields[4].stringValue)
            assertArrayEquals(blob, fields[5].blobValue.toByteArray())
            assertEquals("text", fields[6].stringValue)
            assertEquals(text, fields[7].stringValue)
            assertEquals("null", fields[8].stringValue)
            assertEquals(DataCase.DATA_NOT_SET, fields[9].dataCase)
        }
    }
}
