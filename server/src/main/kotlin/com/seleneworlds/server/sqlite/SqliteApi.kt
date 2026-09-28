package com.seleneworlds.server.sqlite

import com.seleneworlds.common.util.Disposable
import com.seleneworlds.server.config.ServerConfig
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types
import java.util.Collections

/** Opens SQLite databases stored in the server's save directory. */
class SqliteApi(private val serverConfig: ServerConfig) : Disposable {
    private val databases = Collections.synchronizedSet(mutableSetOf<SqliteDatabase>())

    fun open(path: String): SqliteDatabase {
        val jdbcUrl = if (path == ":memory:") {
            "jdbc:sqlite::memory:"
        } else {
            val file = serverConfig.resolveSavePath(path)
            file.parentFile?.mkdirs()
            "jdbc:sqlite:${file.absolutePath}"
        }
        val database = SqliteDatabase(DriverManager.getConnection(jdbcUrl)) {
            databases.remove(it)
        }
        databases.add(database)
        return database
    }

    override fun dispose() {
        databases.toList().forEach(SqliteDatabase::close)
        databases.clear()
    }
}

class SqliteDatabase internal constructor(
    private val connection: Connection,
    private val onClose: (SqliteDatabase) -> Unit
) : AutoCloseable {
    val isOpen: Boolean
        get() = !connection.isClosed

    fun execute(sql: String, parameters: List<Any?> = emptyList()): Int =
        prepare(sql, parameters).use { statement -> statement.executeUpdate() }

    fun query(sql: String, parameters: List<Any?> = emptyList()): List<Map<String, Any?>> =
        prepare(sql, parameters).use { statement ->
            statement.executeQuery().use(::readRows)
        }

    fun scalar(sql: String, parameters: List<Any?> = emptyList()): Any? =
        prepare(sql, parameters).use { statement ->
            statement.executeQuery().use { results ->
                if (results.next()) results.getObject(1) else null
            }
        }

    override fun close() {
        if (!connection.isClosed) connection.close()
        onClose(this)
    }

    private fun prepare(sql: String, parameters: List<Any?>): PreparedStatement {
        check(isOpen) { "SQLite database is closed" }
        return connection.prepareStatement(sql).also { statement ->
            parameters.forEachIndexed { index, value -> statement.bind(index + 1, value) }
        }
    }

    private fun PreparedStatement.bind(index: Int, value: Any?) {
        when (value) {
            null -> setNull(index, Types.NULL)
            is Boolean -> setBoolean(index, value)
            is ByteArray -> setBytes(index, value)
            is Byte -> setByte(index, value)
            is Short -> setShort(index, value)
            is Int -> setInt(index, value)
            is Long -> setLong(index, value)
            is Float -> setFloat(index, value)
            is Double -> setDouble(index, value)
            is Number -> setDouble(index, value.toDouble())
            is String -> setString(index, value)
            else -> throw IllegalArgumentException("Unsupported SQLite parameter type: ${value::class.simpleName}")
        }
    }

    private fun readRows(results: ResultSet): List<Map<String, Any?>> {
        val metadata = results.metaData
        return buildList {
            while (results.next()) {
                add(buildMap {
                    for (column in 1..metadata.columnCount) {
                        put(metadata.getColumnLabel(column), results.getObject(column))
                    }
                })
            }
        }
    }
}
