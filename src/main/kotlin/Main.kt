import kotlinx.coroutines.*
import java.io.FileReader
import java.net.MalformedURLException
import java.net.URL
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.system.exitProcess

class Parser(var url: String, val body: String, val listTerms: List<String>) {
    var current = 0
    var start = 0
    var isCollecting = false
    val links: ArrayList<String> = ArrayList()
    var terms: HashMap<String, Int>
    var domain: String

    init {
        val uri = URL(url)
        this.domain = "${uri.protocol}://${uri.host}"
        this.terms = HashMap()
    }

    fun parse() {
        while (!isAtEnd()) {
            skipWhiteSpace()

            val c = body[current]

            if (c == '<') {
                advance()
                parseTag()
            } else {
                advance()
            }
        }
    }

    private fun advance() {
        current++
    }

    private fun isAtEnd(): Boolean {
        return current >= body.length - 1
    }

    private fun skipWhiteSpace() {
        while (!isAtEnd()) {
            val c = body[current]

            if (c == '\n' || c == ' ' || c == '\t') {
                advance()
            } else {
                break
            }
        }

        start = current
    }

    private fun parseTag() {
        skipWhiteSpace()
        if (check('!')) consume('!')
        if (peek(0) == '-' && peek(1) == '-') {
            while (!isAtEnd() && body[current] != '-' && peek(1) != '-' && peek(2) != '>') {
                advance()
            }

            repeat(2) {
                advance()
            }
        }
        skipWhiteSpace()

        var isClosing = false

        if (check('/')) {
            consume('/')
            isClosing = true
        }

        val name = parseIdentifier().lowercase()

        if (name == "script" || name == "style") {
            parseUntilClosing(name)
            return
        }

        if (name == "body") isCollecting = !isClosing

        if (name == "a") parseAnchor()

        while (!isAtEnd() && body[current] != '>') {
            advance()
        }

        advance()

        if (name == "doctype") return

        parseTagBody()
    }

    val quotes = listOf('"', '\'', '`')
    private fun isQuote(): Boolean {
        if (isAtEnd()) return false

        return quotes.contains(body[current])
    }

    private fun parseUntilClosing(tag: String) {
        while (!isAtEnd() && body[current] != '>') {
            advance()
        }

        var matched = false
        while (!matched && !isAtEnd()) {
            while (!isAtEnd() && body[current] != '<' && peek(1) != '/' && !isQuote()) {
                advance()
            }

            if (isQuote()) {
                val quoteChar = body[current]

                advance()

                while (!isAtEnd() && body[current] != quoteChar) {
                    advance()
                }
                advance()
            }

            if (isAtEnd()) break

            if (body[current] == '<' && peek(1) == '/') {
                repeat(2) {
                    advance()
                }

                skipWhiteSpace()

                matched = true
                for (i in tag.indices) {
                    if (body[current] != tag[i]) {
                        matched = false
                        break
                    }
                    advance()
                }

                advance()
            }
        }
    }

    private fun parseAnchor() {
        while (!isAtEnd() && body[current] != '>') {
            skipWhiteSpace()
            val attribute = parseIdentifier()
            if (check('=')) {
                consume('=')
                var attributeValue = parseValue()

                if (attribute.lowercase() == "href" && attributeValue.isNotEmpty()) {
                    if (attributeValue.startsWith('/')) {
                        attributeValue = domain + attributeValue
                    }

                    if (isUrl(attributeValue)) {
                        links.add(attributeValue)
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

        val tagContent = body.substring(start, current).trim()

        if (tagContent.isNotEmpty() && isCollecting) {
            listTerms.forEach {
                if (tagContent.contains(it)) {
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

    private fun peek(index: Int): Char {
        if (current + index > body.length - 1) return Char(0)

        return body[current + index]
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

    val terms = args
        .sliceArray(1 until args.size)
        .toList()

    val body = FileReader("source.html").readText() // debug way

//    val body = URL(url).readText()

    val parser = Parser(url, body, terms)
    parser.parse()


    parser.links.toList()
    println(parser.terms)
}