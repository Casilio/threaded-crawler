import kotlinx.coroutines.*
import java.net.MalformedURLException
import java.net.URL
import kotlin.system.exitProcess

class Parser(var url: String, val body: String, val listTerms: List<String>) {
    var current = 0
    var start = 0
    var isCollecting = false
    val links: ArrayList<String> = ArrayList<String>()
    var terms: HashMap<String, Int>
    var domain: String

    init {
        val uri = URL(url)
        this.domain = "${uri.protocol}://${uri.host}"
        this.terms = HashMap()
    }

    fun parse() {
        // TODO: skip comments
        while (!isAtEnd()) {
            skipWhiteSpace()

            val c = body[current]

            if (c == '<') {
                advance()
                parseTag()
            }
        }
    }

    private fun advance() {
        current++
    }

    private fun isAtEnd(): Boolean {
        return body.length == current
    }

    private fun skipWhiteSpace() {
        while (!isAtEnd()) {
            val c = body[current]

            if (c == '\n' || c == ' ' || c == '\t') advance() else break
        }

        start = current
    }

    private fun parseTag() {
        skipWhiteSpace()
        if (check('!')) consume('!')
        skipWhiteSpace()

        var isClosing = false

        if (check('/')) {
            consume('/')
            isClosing = true
        }

        val name = parseIdentifier()

        if (name.lowercase() == "body") isCollecting = !isClosing

        if (name.lowercase() == "a") parseAnchor()

        while (!isAtEnd() && body[current] != '>') {
            advance()
        }

        advance()

        if (name.lowercase() == "doctype") return

        parseTagBody()
    }

    private fun parseAnchor() {
        while (!isAtEnd() && body[current] != '>') {
            skipWhiteSpace()
            val attribute = parseIdentifier()
            if (check('=')) {
                consume('=')
                var value = parseValue()

                if (attribute.lowercase() == "href" && value.isNotEmpty()) {
                    if (value.startsWith('/')) {
                        value = domain + value
                        if (isUrl(value)) {
                            links.add(value)
                        }
                    }
                }
            }

            advance()
        }
    }

    private fun parseValue(): String {
        consume('"')
        start = current
        while (!isAtEnd() && body[current] != '"') {
            advance()
        }

        return body.substring(start, current)
    }

    private fun parseTagBody() {
        start = current
        while (!isAtEnd() && body[current] != '<') {
            advance()
        }

        val meat = body.substring(start, current).trim()

        if (meat.isNotEmpty() && isCollecting) {
            listTerms.forEach {
                if (meat.contains(it)) {
                    terms[it] = (terms[it] ?: 0) + 1
                }
            }
        }
    }

    private fun parseIdentifier(): String {
        val start = current
        while (Character.isAlphabetic(body[current].code)) advance()

        return body.substring(start, current)
    }

    private fun check(char: Char): Boolean {
        return body[current] == char
    }

    private fun consume(char: Char) {
        if (body[current] != char)
            throw java.lang.RuntimeException(
                "Wtf is going on. I want to eat $char" +
                        " but got ${body[current]}, at $current"
            )

        advance()
    }

    init {
        val uri = URL(url)
        this.domain = "${uri.protocol}://${uri.host}"
        this.terms = HashMap()
    }
}

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

    val terms = args
        .sliceArray(1 until args.size)
        .map {
            if (it[0] == '"' && it[it.length - 1] == '"') {
                it.substring(1, it.length - 2)
            } else {
                it
            }
        }
        .toList()

//    val body = FileReader("source.html").readText() // debug way

    val body = URL(url).readText()

    val parser = Parser(url, body, terms)
    parser.parse()

    println(parser.links.toList())
    println(parser.terms)
}