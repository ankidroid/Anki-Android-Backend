// SPDX-License-Identifier: GPL-3.0-or-later
package net.ankiweb

import androidx.test.ext.junit.runners.AndroidJUnit4
import anki.ankidroid.SqlValue.DataCase
import net.ankiweb.rsdroid.BackendException
import net.ankiweb.rsdroid.BackendFactory.getBackend
import net.ankiweb.rsdroid.testing.RustBackendLoader.ensureSetup
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

/** Covers argument types that Android's org.json used to convert for us. */
@RunWith(AndroidJUnit4::class)
class DbRequestJsonCompatibilityTest {
    @Test
    fun arraysAndCollectionsRemainBlobs() {
        val arguments =
            listOf(
                intArrayOf(0, 128, 255),
                longArrayOf(0, 128, 255),
                shortArrayOf(0, 128, 255),
                arrayOf(0, 128, 255),
                listOf(0, 128, 255),
                linkedSetOf(0, 128, 255),
                JSONArray(listOf(0, 128, 255)),
                object : JSONArray(listOf(0, 128, 255)) {},
            )
        ensureSetup()
        getBackend().use { backend ->
            backend.openCollection(":memory:")
            for (argument in arguments) {
                val result = backend.fullQueryProto("SELECT typeof(?1), hex(?1)", arrayOf(argument))
                val fields = result.result.getRows(0).fieldsList
                assertEquals(argument.javaClass.name, "blob", fields[0].stringValue)
                assertEquals(argument.javaClass.name, "0080FF", fields[1].stringValue)
            }
        }
    }

    @Test
    fun jsonNullAndUnsupportedObjectsRemainNull() {
        val arguments =
            listOf(
                JSONObject.NULL,
                object {
                    override fun toString() = "should not be bound as text"
                },
            )
        ensureSetup()
        getBackend().use { backend ->
            backend.openCollection(":memory:")
            for (argument in arguments) {
                val result = backend.fullQueryProto("SELECT typeof(?1), ?1", arrayOf(argument))
                val fields = result.result.getRows(0).fieldsList
                assertEquals(argument.javaClass.name, "null", fields[0].stringValue)
                assertEquals(argument.javaClass.name, DataCase.DATA_NOT_SET, fields[1].dataCase)
            }
        }
    }

    @Test
    fun mapsAndJsonObjectsAreStillRejected() {
        val arguments = listOf(mapOf("value" to 1), JSONObject().put("value", 1))
        ensureSetup()
        getBackend().use { backend ->
            backend.openCollection(":memory:")
            for (argument in arguments) {
                assertThrows(BackendException::class.java) {
                    backend.fullQueryProto("SELECT ?", arrayOf(argument))
                }
            }
        }
    }
}
