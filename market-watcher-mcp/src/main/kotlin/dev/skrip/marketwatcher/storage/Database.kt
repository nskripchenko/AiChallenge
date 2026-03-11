package dev.skrip.marketwatcher.storage

import java.sql.Connection
import java.sql.DriverManager
import java.time.Instant

object Database {
    private const val DB_PATH = "market_watcher.db"
    private lateinit var connection: Connection

    fun init() {
        connection = DriverManager.getConnection("jdbc:sqlite:$DB_PATH")
        createTables()
    }

    private fun createTables() {
        connection.createStatement().use { stmt ->
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS watch_jobs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    symbol TEXT NOT NULL UNIQUE,
                    timeframe TEXT NOT NULL,
                    interval_seconds INTEGER NOT NULL,
                    is_active INTEGER NOT NULL DEFAULT 1,
                    created_at TEXT NOT NULL,
                    last_run_at TEXT
                )
            """.trimIndent())

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS market_snapshots (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    symbol TEXT NOT NULL,
                    timeframe TEXT NOT NULL,
                    snapshot_time TEXT NOT NULL,
                    open_price REAL NOT NULL,
                    high_price REAL NOT NULL,
                    low_price REAL NOT NULL,
                    close_price REAL NOT NULL,
                    volume REAL NOT NULL
                )
            """.trimIndent())

            stmt.executeUpdate("""
                CREATE INDEX IF NOT EXISTS idx_snapshots_symbol ON market_snapshots(symbol)
            """.trimIndent())
        }
    }

    fun getActiveJobs(): List<WatchJob> {
        val jobs = mutableListOf<WatchJob>()
        connection.prepareStatement(
            "SELECT id, symbol, timeframe, interval_seconds, is_active, created_at, last_run_at FROM watch_jobs WHERE is_active = 1"
        ).use { stmt ->
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    jobs.add(WatchJob(
                        id = rs.getLong("id"),
                        symbol = rs.getString("symbol"),
                        timeframe = rs.getString("timeframe"),
                        intervalSeconds = rs.getInt("interval_seconds"),
                        isActive = rs.getInt("is_active") == 1,
                        createdAt = rs.getString("created_at"),
                        lastRunAt = rs.getString("last_run_at")
                    ))
                }
            }
        }
        return jobs
    }

    fun getJob(symbol: String): WatchJob? {
        connection.prepareStatement(
            "SELECT id, symbol, timeframe, interval_seconds, is_active, created_at, last_run_at FROM watch_jobs WHERE symbol = ?"
        ).use { stmt ->
            stmt.setString(1, symbol)
            stmt.executeQuery().use { rs ->
                if (rs.next()) {
                    return WatchJob(
                        id = rs.getLong("id"),
                        symbol = rs.getString("symbol"),
                        timeframe = rs.getString("timeframe"),
                        intervalSeconds = rs.getInt("interval_seconds"),
                        isActive = rs.getInt("is_active") == 1,
                        createdAt = rs.getString("created_at"),
                        lastRunAt = rs.getString("last_run_at")
                    )
                }
            }
        }
        return null
    }

    fun createOrUpdateJob(symbol: String, timeframe: String, intervalSeconds: Int): WatchJob {
        val existing = getJob(symbol)
        val now = Instant.now().toString()

        if (existing != null) {
            connection.prepareStatement(
                "UPDATE watch_jobs SET timeframe = ?, interval_seconds = ?, is_active = 1 WHERE symbol = ?"
            ).use { stmt ->
                stmt.setString(1, timeframe)
                stmt.setInt(2, intervalSeconds)
                stmt.setString(3, symbol)
                stmt.executeUpdate()
            }
        } else {
            connection.prepareStatement(
                "INSERT INTO watch_jobs (symbol, timeframe, interval_seconds, is_active, created_at) VALUES (?, ?, ?, 1, ?)"
            ).use { stmt ->
                stmt.setString(1, symbol)
                stmt.setString(2, timeframe)
                stmt.setInt(3, intervalSeconds)
                stmt.setString(4, now)
                stmt.executeUpdate()
            }
        }
        return getJob(symbol)!!
    }

    fun deactivateJob(symbol: String): Boolean {
        connection.prepareStatement(
            "UPDATE watch_jobs SET is_active = 0 WHERE symbol = ?"
        ).use { stmt ->
            stmt.setString(1, symbol)
            return stmt.executeUpdate() > 0
        }
    }

    fun updateLastRunAt(symbol: String) {
        connection.prepareStatement(
            "UPDATE watch_jobs SET last_run_at = ? WHERE symbol = ?"
        ).use { stmt ->
            stmt.setString(1, Instant.now().toString())
            stmt.setString(2, symbol)
            stmt.executeUpdate()
        }
    }

    fun saveSnapshot(snapshot: MarketSnapshot) {
        connection.prepareStatement(
            "INSERT INTO market_snapshots (symbol, timeframe, snapshot_time, open_price, high_price, low_price, close_price, volume) VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
        ).use { stmt ->
            stmt.setString(1, snapshot.symbol)
            stmt.setString(2, snapshot.timeframe)
            stmt.setString(3, snapshot.snapshotTime)
            stmt.setDouble(4, snapshot.openPrice)
            stmt.setDouble(5, snapshot.highPrice)
            stmt.setDouble(6, snapshot.lowPrice)
            stmt.setDouble(7, snapshot.closePrice)
            stmt.setDouble(8, snapshot.volume)
            stmt.executeUpdate()
        }
    }

    fun getSnapshots(symbol: String, limit: Int = 100): List<MarketSnapshot> {
        val snapshots = mutableListOf<MarketSnapshot>()
        connection.prepareStatement(
            "SELECT * FROM market_snapshots WHERE symbol = ? ORDER BY snapshot_time DESC LIMIT ?"
        ).use { stmt ->
            stmt.setString(1, symbol)
            stmt.setInt(2, limit)
            stmt.executeQuery().use { rs ->
                while (rs.next()) {
                    snapshots.add(MarketSnapshot(
                        id = rs.getLong("id"),
                        symbol = rs.getString("symbol"),
                        timeframe = rs.getString("timeframe"),
                        snapshotTime = rs.getString("snapshot_time"),
                        openPrice = rs.getDouble("open_price"),
                        highPrice = rs.getDouble("high_price"),
                        lowPrice = rs.getDouble("low_price"),
                        closePrice = rs.getDouble("close_price"),
                        volume = rs.getDouble("volume")
                    ))
                }
            }
        }
        return snapshots.reversed()
    }

    fun getSnapshotCount(symbol: String): Int {
        connection.prepareStatement(
            "SELECT COUNT(*) FROM market_snapshots WHERE symbol = ?"
        ).use { stmt ->
            stmt.setString(1, symbol)
            stmt.executeQuery().use { rs ->
                if (rs.next()) {
                    return rs.getInt(1)
                }
            }
        }
        return 0
    }
}

data class WatchJob(
    val id: Long,
    val symbol: String,
    val timeframe: String,
    val intervalSeconds: Int,
    val isActive: Boolean,
    val createdAt: String,
    val lastRunAt: String?
)

data class MarketSnapshot(
    val id: Long = 0,
    val symbol: String,
    val timeframe: String,
    val snapshotTime: String,
    val openPrice: Double,
    val highPrice: Double,
    val lowPrice: Double,
    val closePrice: Double,
    val volume: Double
)
