import kotlinx.coroutines.*
import java.io.FileReader
import java.net.MalformedURLException
import java.net.URL
import java.util.*
import kotlin.collections.HashMap
import kotlin.system.exitProcess


// suspend fun dosome(flags: Int) = coroutineScope {
//     var dispatcher = Dispatchers.IO.limitedParallelism(30)
//     val result = (1..30).map {
//         async(dispatcher) {
//             println(it)
//             URL("https://google.com/").readText()
//         }
//
//     } .map {
//         it.await()
//     }.joinToString("\n")
//
//     println(result)
// }

fun isUrl(url: String): Boolean {
    return try {
        URL(url)
        true
    } catch (e: MalformedURLException) {
        false
    }
}

data class Result(val url: String, val map: HashMap<String, Int>, val totalMatches: Int)

const val MAX_DEPTH = 8
const val MAX_RESULTS = 100

var terms: List<String>? = null
val urlsToProcess = Stack<Pair<String, Int>>()
val results = HashMap<String, Result?>()

fun processNextUrl() {
    if (urlsToProcess.empty()) return

    val parentPair = urlsToProcess.pop()

    val uri = URL(parentPair.first)
    val domain = "${uri.protocol}://${uri.host}"
    val map = HashMap<String, Int>(terms!!.size)

    val body = if (System.getenv("DEBUG") == "1") {
        FileReader("source.html").readText()
    } else {
        println("Fetching ${parentPair.first}. Depth: ${parentPair.second}")
        URL(parentPair.first).readText()
    }

    Parser(body, object: Consumer{
        override fun onContent(content: String) {
            terms!!.forEach {
                // TODO: record actual matches count instead of incrementing once
                if (content.contains(it)) {
                    map[it] = (map[it] ?: 0) + 1
                }
            }
        }

        override fun onLink(link: String) {
            var url = link
            if (link.startsWith('/')) {
                url = domain + link
            }

            if (parentPair.second + 1 > MAX_DEPTH) return
            if (results.size - 1 >= MAX_RESULTS) return
            if (!isUrl(url)) return
            if (results.containsKey(url)) return

            results[url] = null
            urlsToProcess.push(Pair(url, parentPair.second + 1))
        }
    }).parse()

    results[parentPair.first] = Result(parentPair.first, map, map.values.fold(0) { acc, i -> acc + i })
}

fun printHelp() {
    println("Usage: crawler url term [term]...")
}

fun main(args: Array<String>) {
    if (args.size < 2) {
        printHelp()
        exitProcess(1)
    }

    val url = args[0]

    if (!isUrl(url)) {
        System.err.println("Expect first argument to be a url.")
        exitProcess(2)
    }

    terms = args
        .sliceArray(1 until args.size)
        .toList()

    urlsToProcess.push(Pair(url, 1))

    while (urlsToProcess.isNotEmpty()) {
        processNextUrl()
    }

    println(results.size)
    for (i in results) {
        println(i.key)
        println(i.value!!.totalMatches)
        println(i.value!!.map)
    }
}