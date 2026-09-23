// SPDX-License-Identifier: GPL-3.0-or-later
package net.ankiweb

import androidx.test.ext.junit.runners.AndroidJUnit4
import net.ankiweb.rsdroid.Backend
import net.ankiweb.rsdroid.BackendException
import net.ankiweb.rsdroid.BackendFactory.getBackend
import net.ankiweb.rsdroid.database.SQLHandler
import net.ankiweb.rsdroid.testing.RustBackendLoader.ensureSetup
import org.json.JSONObject
import org.junit.Assert.assertEquals
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
            assertEquals(1, rows.length())
            val row = rows.getJSONArray(0)
            assertEquals(6, row.length())
            // Numeric accessors also accept quoted numbers, so check JSON types separately.
            assertTrue(row.get(0) is Number)
            assertTrue(row.get(1) is Number)
            assertTrue(row.get(2) is Number)
            assertTrue(row.get(3) is String)
            assertEquals(Long.MIN_VALUE, row.getLong(0))
            assertEquals(Long.MAX_VALUE, row.getLong(1))
            assertEquals(1.25, row.getDouble(2), 0.0)
            assertEquals(text, row.getString(3))
            assertEquals(JSONObject.NULL, row.get(4))
            val blob = row.getJSONArray(5)
            assertEquals(listOf(0, 128, 255), (0 until blob.length()).map { blob.getInt(it) })
            assertTrue((0 until blob.length()).all { blob.get(it) is Number })
        }
    }

    @Test
    fun noArgumentOverloadsAndEmptyResultsWork() {
        withBackend { backend ->
            val database: SQLHandler = backend
            val expected = backend.fullQuery("SELECT 42", emptyArray())
            assertEquals(1, expected.length())
            assertEquals(42, expected.getJSONArray(0).getInt(0))
            assertEquals(expected.toString(), database.fullQuery("SELECT 42").toString())
            assertEquals(expected.toString(), database.fullQuery("SELECT 42", null).toString())
            assertEquals(0, database.fullQuery("SELECT 42 WHERE 0").length())
        }
    }

    @Test
    fun resultsRemainReadableAfterClosingBackend() {
        val rows =
            getBackend().use { backend ->
                backend.openCollection(":memory:")
                backend.fullQuery("SELECT 1 AS value UNION ALL SELECT 2 ORDER BY value")
            }
        assertEquals(listOf(1, 2), (0 until rows.length()).map { rows.getJSONArray(it).getInt(0) })
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
                assertEquals(listOf(1, 2, 3), (0 until rows.length()).map { rows.getJSONArray(it).getInt(0) })
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
                assertEquals(0, backend.fullQuery("SELECT * FROM missing_table").length())
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
