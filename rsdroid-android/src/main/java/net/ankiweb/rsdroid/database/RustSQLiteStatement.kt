// SPDX-License-Identifier: GPL-3.0-or-later

package net.ankiweb.rsdroid.database

import android.database.sqlite.SQLiteDoneException
import androidx.sqlite.db.SupportSQLiteStatement

class RustSQLiteStatement(
    private val database: RustSupportSQLiteDatabase,
    private val sql: String,
) : SupportSQLiteStatement {
    private val mBindings = HashMap<Int, Any?>()

    override fun execute() {
        database.query(sql, bindings).close()
    }

    override fun executeUpdateDelete(): Int = database.executeGetRowsAffected(sql, bindings)

    override fun executeInsert(): Long = database.insertForForId(sql, bindings)

    override fun simpleQueryForLong(): Long {
        database.query(sql, bindings).use { query ->
            if (!query.moveToFirst()) {
                throw SQLiteDoneException()
            }
            return query.getLong(0)
        }
    }

    override fun simpleQueryForString(): String {
        database.query(sql, bindings).use { query ->
            if (!query.moveToFirst()) {
                throw SQLiteDoneException()
            }
            return query.getString(0)
        }
    }

    override fun bindNull(index: Int) {
        bind(index, null)
    }

    override fun bindLong(
        index: Int,
        value: Long,
    ) {
        bind(index, value)
    }

    override fun bindDouble(
        index: Int,
        value: Double,
    ) {
        bind(index, value)
    }

    override fun bindString(
        index: Int,
        value: String,
    ) {
        bind(index, value)
    }

    override fun bindBlob(
        index: Int,
        value: ByteArray,
    ) {
        bind(index, value)
    }

    override fun clearBindings() {
        mBindings.clear()
    }

    override fun close() {}

    private fun bind(
        index: Int,
        value: Any?,
    ) {
        mBindings[index] = value
    }

    val bindings: Array<Any?>
        get() {
            val max = max(mBindings.keys)
            val ret = arrayOfNulls<Any>(max + 1)
            for (i in 0..max) {
                if (mBindings.containsKey(i)) {
                    ret[i] = mBindings[i]
                }
            }
            return ret
        }

    private fun max(integerSet: Set<Int>): Int {
        var max = -1
        for (i in integerSet) {
            max = kotlin.math.max(max, i)
        }
        return max
    }
}
