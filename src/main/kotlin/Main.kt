import kotlinx.coroutines.*
import java.net.MalformedURLException
import java.net.URL

fun extractUrls(body: String) {
    Regex("<a.*href=\"(.+?\")")
        .findAll(body)
        .map { it.groups[1]?.value.toString() }
        .filter {
            try {
                URL(it)
                true
            } catch (e: MalformedURLException) {
                false
            }
        }.toList()
}
suspend fun dosome() = coroutineScope {
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

fun main(args: Array<String>) = runBlocking {
//    val start = System.currentTimeMillis()
//    dosome()
//    val finish = System.currentTimeMillis()

//    println(finish - start)
}