// SPDX-License-Identifier: GPL-3.0-or-later

package net.ankiweb

import net.ankiweb.rsdroid.dbRequestJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * Tests the JSON sent to the backend.
 * `/` is no longer escaped; serde treats it equivalently.
 */
class DbRequestJsonTest {
    @Test
    fun requestMatchesOrgJsonFormat() {
        // The backend rejects booleans, but their JSON should match the previous output.
        val json =
            dbRequestJson(
                "select ?, ?, ?, ?, ?, ?",
                arrayOf<Any?>(null, "quote\" emoji🎴", 42L, 1.0, 2.5, true),
            ).toStringUtf8()
        assertEquals(
            """{"kind":"query","sql":"select ?, ?, ?, ?, ?, ?","args":[null,"quote\" emoji🎴",42,1,2.5,true],"first_row_only":false}""",
            json,
        )
    }

    @Test
    fun wholeDoublesBindAsIntegers() {
        val json = dbRequestJson("select ?", arrayOf<Any?>(3.0)).toStringUtf8()
        assertEquals("""{"kind":"query","sql":"select ?","args":[3],"first_row_only":false}""", json)
    }

    @Test
    fun wholeDoublesAboveTenMillionStillBindAsIntegers() {
        // Double.toString uses scientific notation from 1e7, but org.json wrote
        // whole values as integers. This includes timestamps in milliseconds.
        val json =
            dbRequestJson(
                "select ?, ?, ?",
                arrayOf<Any?>(1.0E7, 1751234567890.0, 9.2233720368547758E18),
            ).toStringUtf8()
        assertEquals(
            """{"kind":"query","sql":"select ?, ?, ?","args":[10000000,1751234567890,9223372036854775807],"first_row_only":false}""",
            json,
        )
    }

    @Test
    fun floatsMatchOrgJson() {
        val json = dbRequestJson("select ?, ?, ?, ?", arrayOf<Any?>(1.5f, 2.0f, 1.0E8f, 0.1f)).toStringUtf8()
        assertEquals(
            """{"kind":"query","sql":"select ?, ?, ?, ?","args":[1.5,2,100000000,0.1],"first_row_only":false}""",
            json,
        )
    }

    @Test
    fun negativeZeroMatchesOrgJson() {
        // org.json wrote Double -0.0 as -0, but Float -0.0f as 0.
        val json = dbRequestJson("select ?, ?", arrayOf<Any?>(-0.0, -0.0f)).toStringUtf8()
        assertEquals("""{"kind":"query","sql":"select ?, ?","args":[-0,0],"first_row_only":false}""", json)
    }

    @Test
    fun positiveZeroMatchesOrgJson() {
        val json = dbRequestJson("select ?, ?", arrayOf<Any?>(0.0, 0.0f)).toStringUtf8()
        assertEquals("""{"kind":"query","sql":"select ?, ?","args":[0,0],"first_row_only":false}""", json)
    }

    @Test
    fun longLimitsArePreserved() {
        val json =
            dbRequestJson(
                "select ?, ?, ?",
                arrayOf<Any?>(-1L, Long.MAX_VALUE, Long.MIN_VALUE),
            ).toStringUtf8()
        assertEquals(
            """{"kind":"query","sql":"select ?, ?, ?","args":[-1,9223372036854775807,-9223372036854775808],"first_row_only":false}""",
            json,
        )
    }

    @Test
    fun stringsAreEscapedCorrectly() {
        val json =
            dbRequestJson(
                "select ?, ?",
                arrayOf<Any?>("a/b", "line\nbreak\ttab\\bs\u0001"),
            ).toStringUtf8()
        assertEquals(
            """{"kind":"query","sql":"select ?, ?","args":["a/b","line\nbreak\ttab\\bs\u0001"],"first_row_only":false}""",
            json,
        )
    }

    @Test
    fun nonFiniteNumbersAreRejected() {
        for (value in listOf<Any>(Double.NaN, Double.POSITIVE_INFINITY, Float.NaN)) {
            assertThrows(IllegalArgumentException::class.java) {
                dbRequestJson("select ?", arrayOf<Any?>(value))
            }
        }
    }

    @Test
    fun blobsKeepTheirArrayEncoding() {
        val json = dbRequestJson("select ?", arrayOf<Any?>(byteArrayOf(0, 1, 127))).toStringUtf8()
        assertEquals(
            """{"kind":"query","sql":"select ?","args":[[0,1,127]],"first_row_only":false}""",
            json,
        )
    }

    @Test
    fun firstRowOnlyIsSet() {
        val json = dbRequestJson("select 1", emptyArray(), firstRowOnly = true).toStringUtf8()
        assertEquals("""{"kind":"query","sql":"select 1","args":[],"first_row_only":true}""", json)
    }
}
