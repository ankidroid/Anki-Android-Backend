// SPDX-License-Identifier: GPL-3.0-or-later
package net.ankiweb.rsdroid.testing

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ClassLoaderLibraryCopyTest {
    @Test
    fun interruptedCleanupPreservesInterruptAndReportsFailure() {
        try {
            Thread.currentThread().interrupt()
            assertFalse(awaitClassLoaderCleanup())
            assertTrue(Thread.currentThread().isInterrupted)
        } finally {
            Thread.interrupted()
        }
    }
}
