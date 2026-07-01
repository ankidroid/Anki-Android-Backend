// SPDX-License-Identifier: GPL-3.0-or-later

package org.slf4j.impl

import org.slf4j.ILoggerFactory
import org.slf4j.Logger
import org.slf4j.helpers.NOPLogger

/**
 * A minimal SLF4J 1.7-style binding
 *
 * This serves two purposes:
 * * silences logs in unit tests
 * * Allows verification that 1.7-style bindings are used
 */
@Suppress("unused") // fields/methods are loaded by slf4j-api
class StaticLoggerBinder private constructor() {
    val loggerFactory: ILoggerFactory = TestBindingLoggerFactory()

    fun getLoggerFactoryClassStr(): String = TestBindingLoggerFactory::class.java.name

    companion object {
        private val SINGLETON = StaticLoggerBinder()

        @JvmStatic
        fun getSingleton(): StaticLoggerBinder = SINGLETON

        // read via a static field reference by slf4j-api's version sanity check
        const val REQUESTED_API_VERSION: String = "1.7.36"
    }
}

/** Discards all log output like slf4j-nop, but with a distinct type tests can assert on */
class TestBindingLoggerFactory : ILoggerFactory {
    override fun getLogger(name: String): Logger = NOPLogger.NOP_LOGGER
}
