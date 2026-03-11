package dev.skrip.marketwatcher.scheduler

import dev.skrip.marketwatcher.market.MarketFetcher
import dev.skrip.marketwatcher.storage.Database
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

object WatcherScheduler {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val activeJobs = ConcurrentHashMap<String, Job>()

    @Volatile
    private var isRunning = false

    fun start() {
        if (isRunning) return
        isRunning = true

        scope.launch {
            System.err.println("[Scheduler] Starting background scheduler")
            Database.getActiveJobs().forEach { job ->
                startWatchJob(job.symbol, job.timeframe, job.intervalSeconds)
            }
        }
    }

    fun startWatchJob(symbol: String, timeframe: String, intervalSeconds: Int) {
        activeJobs[symbol]?.cancel()

        val job = scope.launch {
            System.err.println("[Scheduler] Starting watch job for $symbol ($timeframe) every ${intervalSeconds}s")
            while (isActive) {
                try {
                    val snapshot = MarketFetcher.fetchKline(symbol, timeframe)
                    if (snapshot != null) {
                        Database.saveSnapshot(snapshot)
                        Database.updateLastRunAt(symbol)
                        System.err.println("[Scheduler] Snapshot saved for $symbol: close=${snapshot.closePrice}")
                    }
                } catch (e: Exception) {
                    System.err.println("[Scheduler] Error in watch job for $symbol: ${e.message}")
                }
                delay(intervalSeconds * 1000L)
            }
        }
        activeJobs[symbol] = job
    }

    fun stopWatchJob(symbol: String) {
        activeJobs[symbol]?.cancel()
        activeJobs.remove(symbol)
        System.err.println("[Scheduler] Stopped watch job for $symbol")
    }

    fun isJobActive(symbol: String): Boolean {
        return activeJobs[symbol]?.isActive == true
    }

    fun shutdown() {
        isRunning = false
        scope.cancel()
        MarketFetcher.close()
        System.err.println("[Scheduler] Shutdown complete")
    }
}
