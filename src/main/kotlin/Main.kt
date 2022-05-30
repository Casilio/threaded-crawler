import kotlinx.coroutines.*
import java.net.MalformedURLException
import java.net.URL
import kotlin.system.exitProcess

fun extractUrls(body: String) {
    Regex("<a.*href=\"(.+?\")")
        .findAll(body)
        .map { it.groups[1]?.value.toString() }
        .filter(::isUrl)
        .toList()
}
suspend fun dosome(flags: Int) = coroutineScope {
    var dispatcher = Dispatchers.IO.limitedParallelism(30)
    val result = (1..30).map {
        async(dispatcher) {
            println(it)
            URL("https://google.com/").readText()
        }

    } .map {
        it.await()
    }.joinToString("\n")

    println(result)
}

fun isUrl(url: String): Boolean {
    return try {
        URL(url)
        true
    } catch (e: MalformedURLException) {
        false
    }
}

fun printHelp() {
    println("Usage: crawler url term [term]...")
}

fun main(args: Array<String>) = runBlocking {
    if (args.size < 2) {
        printHelp();
        exitProcess(1);
    }

    val url = args[0]
    println(url)
    println(args.toList())

    if (!isUrl(url)) {
        System.err.println("Expect first argument to be a url.")
        exitProcess(2);
    }

    val terms = args.slice(1..-1)
    println(terms.toList())

//    val start = System.currentTimeMillis()
//    dosome()
//    val finish = System.currentTimeMillis()

//    println(finish - start)
}