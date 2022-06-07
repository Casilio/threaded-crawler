import kotlinx.coroutines.*
import java.io.FileReader
import java.net.MalformedURLException
import java.net.URL
import java.util.*
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

typealias UrlToProcess = Pair<String, Int>
val urlsToProcess = Stack<UrlToProcess>()
val bodiesToProcess = Stack<Pair<String, UrlToProcess>>()
val results = HashMap<String, Result?>()

suspend fun fetch() = coroutineScope {
    while (synchronized(urlsToProcess) { urlsToProcess.isNotEmpty() }) {
        launch(Dispatchers.IO) {
            synchronized(urlsToProcess) {
                if (urlsToProcess.isEmpty()) return@launch
            }

            val urlToProcess = urlsToProcess.pop()
            val (url, depth) = urlToProcess

            val body = if (System.getenv("DEBUG") == "1") {
                withContext(Dispatchers.IO) {
                    FileReader("source.html").readText()
                }
            } else {
                println("Fetching ${url}. Depth: $depth")
                URL(url).readText()
            }

            synchronized(bodiesToProcess) {
                bodiesToProcess.push(Pair(body, urlToProcess))
            }
        }
    }
}

suspend fun parse() = coroutineScope {
    while (synchronized(bodiesToProcess) { bodiesToProcess.isNotEmpty() }) {
        launch(Dispatchers.IO) {
            val (body, meta) = bodiesToProcess.pop()
            val (url, depth) = meta

            val map = HashMap<String, Int>(terms!!.size)

            val uri = URL(url)
            val domain = "${uri.protocol}://${uri.host}"

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
                    var nextUrl = link
                    if (link.startsWith('/')) {
                        nextUrl = domain + link
                    }

                    if (depth + 1 > MAX_DEPTH) return
                    if (results.size - 1 >= MAX_RESULTS) return
                    if (!isUrl(nextUrl)) return
                    if (results.containsKey(nextUrl)) return

                    results[nextUrl] = null
                    urlsToProcess.push(Pair(nextUrl, depth + 1))
                }
            }).parse()

            results[url] = Result(url, map, map.values.fold(0) { acc, i -> acc + i })
        }
    }
}
fun printHelp() {
    println("Usage: crawler url term [term]...")
}

fun main(args: Array<String>) = runBlocking {
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

    while (urlsToProcess.isNotEmpty() || bodiesToProcess.isNotEmpty()) {
        fetch()
        parse()
    }

    println(results.size)
    for ((k,v) in results) {
        println(k)
        println(v!!.totalMatches)
        println(v.map)
    }
}