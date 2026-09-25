// SPDX-License-Identifier: GPL-3.0-or-later
package net.ankiweb

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import net.ankiweb.rsdroid.Backend
import net.ankiweb.rsdroid.BackendException
import net.ankiweb.rsdroid.BackendFactory.getBackend
import net.ankiweb.rsdroid.database.SQLHandler
import net.ankiweb.rsdroid.testing.RustBackendLoader.ensureSetup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FullQueryTest {
    @Before
    fun loadLibrary() {
        ensureSetup()
    }

    @Test
    fun sqlValuesAreReturnedWithoutLosingPrecision() {
        withBackend { backend ->
            val text = "quote\" slash/line\n🎴"
            val rows =
                backend.fullQuery(
                    "SELECT ?, ?, ?, ?, ?, x'0080ff'",
                    arrayOf(Long.MIN_VALUE, Long.MAX_VALUE, 1.25, text, null),
                )
            assertEquals(1, rows.size)
            val row = rows[0].jsonArray
            assertEquals(6, row.size)
            // Numeric accessors also accept quoted numbers, so check JSON types separately.
            assertFalse(row[0].jsonPrimitive.isString)
            assertFalse(row[1].jsonPrimitive.isString)
            assertFalse(row[2].jsonPrimitive.isString)
            assertTrue(row[3].jsonPrimitive.isString)
            assertEquals(Long.MIN_VALUE, row[0].jsonPrimitive.long)
            assertEquals(Long.MAX_VALUE, row[1].jsonPrimitive.long)
            assertEquals(1.25, row[2].jsonPrimitive.double, 0.0)
            assertEquals(text, row[3].jsonPrimitive.content)
            assertEquals(JsonNull, row[4])
            assertEquals(listOf(0, 128, 255), row[5].jsonArray.map { it.jsonPrimitive.int })
            assertTrue(row[5].jsonArray.all { !it.jsonPrimitive.isString })
        }
    }

    @Test
    fun noArgumentOverloadsAndEmptyResultsWork() {
        withBackend { backend ->
            val database: SQLHandler = backend
            val expected = backend.fullQuery("SELECT 42", emptyArray())
            assertEquals(1, expected.size)
            assertEquals(42, expected[0].jsonArray[0].jsonPrimitive.int)
            assertEquals(expected, database.fullQuery("SELECT 42"))
            assertEquals(expected, database.fullQuery("SELECT 42", null))
            assertTrue(database.fullQuery("SELECT 42 WHERE 0").isEmpty())
        }
    }

    @Test
    fun resultsRemainReadableAfterClosingBackend() {
        val rows =
            getBackend().use { backend ->
                backend.openCollection(":memory:")
                backend.fullQuery("SELECT 1 AS value UNION ALL SELECT 2 ORDER BY value")
            }
        assertEquals(listOf(1, 2), rows.map { it.jsonArray[0].jsonPrimitive.int })
    }

    @Test
    fun fullResultDoesNotDependOnPageSizeOrLeaveCachedPages() {
        withBackend { backend ->
            backend.setPageSize(9)
            try {
                val rows =
                    backend.fullQuery(
                        "SELECT 1 AS value UNION ALL SELECT 2 UNION ALL SELECT 3 ORDER BY value",
                    )
                assertEquals(listOf(1, 2, 3), rows.map { it.jsonArray[0].jsonPrimitive.int })
                assertTrue(backend.getActiveSequenceNumbers().isEmpty())
            } finally {
                // Page size is shared by all backend instances; restore the default.
                backend.setPageSize(2 * 1024 * 1024)
            }
        }
    }

    @Test
    fun databaseErrorsRemainBackendExceptions() {
        withBackend { backend ->
            assertThrows(BackendException::class.java) {
                assertTrue(backend.fullQuery("SELECT * FROM missing_table").isEmpty())
            }
        }
    }

    private fun withBackend(block: (Backend) -> Unit) {
        getBackend().use { backend ->
            backend.openCollection(":memory:")
            block(backend)
        }
    }
}
