interface Consumer {
    fun onLink(link: String)
    fun onContent(content: String)
}

val quotes = listOf('"', '\'', '`')

class Parser(private val body: String, private val consumer: Consumer) {
    var current = 0
    var doMatching = false

    fun parse() {
        current = 0

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
    }

    private fun parseTag() {
        skipWhiteSpace()

        // doctype check
        if (check('!')) consume('!')

        // comments check
        if (lookAhead(0) == '-' && lookAhead(1) == '-') {
            while (!isAtEnd() && body[current] != '-' && lookAhead(1) != '-' && lookAhead(2) != '>') {
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

        if (name == "body") doMatching = !isClosing

        if (name == "a") parseAnchor()

        while (!isAtEnd() && body[current] != '>') {
            advance()
        }

        advance()

        if (name == "doctype") return

        parseTagContent()
    }

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
            advance()

            if (isQuote()) {
                val quoteChar = body[current]

                advance()

                while (!isAtEnd() && body[current] != quoteChar) {
                    advance()
                }
                advance()
            }

            if (isAtEnd()) break

            if (body[current] == '<' && lookAhead(1) == '/') {
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
                val attributeValue = parseValue()

                if (attribute.lowercase() == "href" && attributeValue.trim().isNotEmpty()) {
                    consumer.onLink(attributeValue)
                }
            }

            advance()
        }
    }

    private fun parseValue(): String {
        consume('"')
        val start = current
        while (!isAtEnd() && body[current] != '"') {
            advance()
        }

        return body.substring(start, current)
    }

    private fun parseTagContent() {
        val start = current
        while (!isAtEnd() && body[current] != '<') {
            advance()
        }

        val tagContent = body.substring(start, current).trim()

        if (tagContent.isNotEmpty() && doMatching) {
            consumer.onContent(tagContent)
        }
    }

    private fun parseIdentifier(): String {
        val start = current
        while (Character.isAlphabetic(body[current].code)) advance()

        return body.substring(start, current)
    }

    private fun lookAhead(index: Int): Char {
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
}
