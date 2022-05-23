import kotlinx.coroutines.*
import java.net.URL
import java.util.Timer


suspend fun dosome() = coroutineScope {
    var dispatcher = Dispatchers.IO.limitedParallelism(30)
    val result = (1..30).map {
        async(dispatcher) {
            println(it)
            URL("https://google.com/").readText()
        }

    } .map {
        it.await()
    }.joinToString("\n") {
        it.substring(0, 10)
    }

    println(result)
}

fun main(args: Array<String>) = runBlocking {
    val start = System.currentTimeMillis()
    dosome()
    val finish = System.currentTimeMillis()


    println(finish - start)
}