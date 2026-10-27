// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright (c) 2022 Ankitects Pty Ltd <http://apps.ankiweb.net>

package net.ankiweb.rsdroid.database

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import net.ankiweb.rsdroid.Backend

/**
 * Helper routines for constructing Rust-backed and Framework-backed
 * SupportSQLiteDatabases.
 */
abstract class AnkiSupportSQLiteDatabase {
    companion object {
        /**
         * Wrap a Rust backend connection (which provides an SQL interface).
         * Caller is responsible for opening&closing the database.
         */
        @JvmStatic
        fun withRustBackend(backend: Backend): SupportSQLiteDatabase = RustSupportSQLiteDatabase(backend)

        /**
         * Open a connection using the Android framework.
         * If path is not provided, an in-memory database is opened.
         */
        @JvmStatic
        @JvmOverloads
        fun withFramework(
            context: Context,
            path: String?,
            dbCallback: SupportSQLiteOpenHelper.Callback? = null,
        ): SupportSQLiteDatabase {
            val configuration =
                SupportSQLiteOpenHelper.Configuration
                    .builder(context)
                    .name(path)
                    .callback(dbCallback ?: DefaultDbCallback(1))
                    .build()
            return FrameworkSQLiteOpenHelperFactory().create(configuration).writableDatabase
        }
    }

    open class DefaultDbCallback(
        version: Int,
    ) : SupportSQLiteOpenHelper.Callback(version) {
        override fun onCreate(db: SupportSQLiteDatabase) {}

        override fun onUpgrade(
            db: SupportSQLiteDatabase,
            oldVersion: Int,
            newVersion: Int,
        ) {}

        override fun onCorruption(db: SupportSQLiteDatabase) {}
    }
}
