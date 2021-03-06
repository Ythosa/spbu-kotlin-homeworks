package finals.wikirace

import finals.wikirace.config.Config
import io.github.fastily.jwiki.core.Wiki
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ForkJoinPool

class WikiRace(private val wikiClient: Wiki, private val config: Config) {
    fun play(): String = runBlocking(ForkJoinPool(config.processCount).asCoroutineDispatcher()) {
        bfs(config.startPage, config.searchTarget).toPath()
    }

    private suspend fun bfs(start: String, target: String): List<String> = coroutineScope {
        val parents = ConcurrentHashMap<String, String>().apply { this[start] = "" }
        val currentLevel = ConcurrentLinkedQueue<String>().apply { add(start) }
        val nextLevel = ConcurrentLinkedQueue<String>()
        var level = 1

        while (!currentLevel.isEmpty() && level <= config.searchDepth) {
            val isTargetFoundChannel = Channel<Boolean>()
            val pageProcessors = currentLevel.map {
                launch {
                    isTargetFoundChannel.send(processPage(it, start, target, nextLevel, parents))
                }
            }
            repeat(currentLevel.size) {
                if (isTargetFoundChannel.receive()) {
                    pageProcessors.forEach { it.cancel() }

                    return@coroutineScope backtrace(parents, start, target)
                }
            }

            currentLevel.clear()
            currentLevel.addAll(nextLevel.toList())
            nextLevel.clear()
            level++
        }

        return@coroutineScope emptyList()
    }

    private fun processPage(
        page: String,
        start: String,
        target: String,
        nextLevel: ConcurrentLinkedQueue<String>,
        parents: ConcurrentHashMap<String, String>
    ): Boolean {
        println("...Processing ${backtrace(parents, start, page).toPath()}")

        if (page == target) {
            return true
        }

        for (link in wikiClient.getLinksOnPageInMain(page)) {
            if (!parents.keys().toList().contains(link)) {
                parents[link] = page
                nextLevel.add(link)
            }
        }

        return false
    }

    private fun backtrace(parents: ConcurrentHashMap<String, String>, start: String, target: String): List<String> {
        val path = mutableListOf(target)

        while (path.last() != start) {
            path.add(parents[path.last()] ?: throw InternalError("error while building backtrace"))
        }

        return path.reversed()
    }

    private fun <T> List<T>.toPath(): String = this.joinToString(" / ") { "\"$it\"" }
}
