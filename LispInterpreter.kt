package com.example.lispinterpreter

fun main() {
    // Example of usage of the interpreter.
    val interpreter = LispInterpreter()
    interpreter.interpret("12")
    interpreter.interpret("()")
    interpreter.interpret("((1 2) ((2) 3))")
}

class LispTokenizer (val lispText: String) {
    fun tokenize(): List<String> {
        val text = this.lispText
        val tokens: MutableList<String> = mutableListOf()
        var startIndex = 0
        var endIndex = 0

        while (endIndex < text.length) {
            val char = text[endIndex]
            if (!Symbols.singleCharToken(char)) {
                endIndex += 1
                continue
            }

            if (startIndex < endIndex) {
                tokens += text.substring(startIndex, endIndex)
            }
            if (char != Symbols.SPACE) {
                tokens += char.toString()
            }
            startIndex = endIndex + 1
            endIndex += 1
        }

        if (startIndex < text.length) {
            tokens += text.substring(startIndex)
        }
        return tokens
    }
}

/**
 * LISP interpreter implementation.
 * To run interpretation, give a LISP code string to [interpret].
 *
 * Roughly speaking, the interpretation consists of three steps:
 * 1. [tokenize]: Split LISP text for each tokens.
 *   e.g., "(1 (2 3))" -> ["(", "1", "(", "2", "3", ")", ")"]
 * 2. [parse]: Group tokens by parentheses.
 *   e.g., ["(", "1", "(", "2", "3", ")", ")"] -> ["1", ["2", "3"]]
 * 3. `evaluate`: (not implemented yet)
 */
class LispInterpreter {

    private val state = ParseState("", 0)

    fun interpret(lispText: String) {
        val tokens = LispTokenizer(lispText).tokenize()
        state.index = 0
        val parseResult = LispParser().parse(tokens)
        // TODO: Implement `evaluation` logic. Currently, just show the parse result.
        println(parseResult)
    }
}

class LispParser {
    var index = 0
    fun parse(tokens: List<String>): ParseResult {
        if (this.index >= tokens.size) {
            return ParseResult(null, SyntaxError.UnexpectedEos)
        }
        val tokenString = tokens[this.index]
        if (tokenString == Symbols.S_CLOSE) {
            return ParseResult(null, SyntaxError.UnexpectedToken(tokenString))
        }

        return when {
            tokenString != Symbols.S_OPEN -> {
                this.index += 1
                ParseResult(ParsedTokens.SingleToken(tokenString), null)
            }

            tokens[this.index + 1] == Symbols.S_CLOSE -> {
                this.index += 2
                ParseResult(ParsedTokens.TokenGroup(listOf()), null)
            }

            else -> {
                this.index += 1
                val parseResult = parse(tokens)
                if (parseResult.tokens != null && parseResult.error == null) {
                    parseTokensInParentheses(parseResult.tokens, tokens)
                } else {
                    parseResult
                }
            }
        }
    }

    private fun parseTokensInParentheses(
        tokens: ParsedTokens,
        tokens0: List<String>
    ): ParseResult {
        val tokensInParentheses: MutableList<ParsedTokens> = mutableListOf(tokens)
        var indexInParentheses: Int = this.index

        while (tokens0[indexInParentheses] != Symbols.S_CLOSE) {
            if (indexInParentheses >= tokens0.size) {
                return ParseResult(null, SyntaxError.UnexpectedEos)
            }

            this.index = indexInParentheses
            val recursiveResult = parse(tokens0)
            if (recursiveResult.tokens == null || recursiveResult.error != null) {
                return recursiveResult
            }

            indexInParentheses = this.index
            tokensInParentheses.add(recursiveResult.tokens)
        }
        this.index = indexInParentheses + 1
        return ParseResult(ParsedTokens.TokenGroup(tokensInParentheses), null)
    }
}

/**
 * Definition of special characters and the corresponding strings:
 * the token separator [SPACE] and parenthesis symbols.
 */
object Symbols {
    const val SPACE = ' '
    private const val OPEN_PARENTHESIS = '('
    private const val CLOSE_PARENTHESIS = ')'

    const val S_OPEN = OPEN_PARENTHESIS.toString()
    const val S_CLOSE = CLOSE_PARENTHESIS.toString()

    private val SYMBOL_CHAR_SET: Set<Char> = setOf(SPACE, OPEN_PARENTHESIS, CLOSE_PARENTHESIS)

    fun singleCharToken(char: Char): Boolean = SYMBOL_CHAR_SET.contains(char)
}

/**
 * Result model of parsing a token list
 *
 * This structure consists of a single token type and a token group type.
 * A token group has models of the same type recursively.
 */
sealed interface ParsedTokens {
    class TokenGroup(val tokens: List<ParsedTokens>) : ParsedTokens {
        override fun toString(): String =
            tokens.joinToString(separator = ", ", prefix = "[", postfix = "]")
    }

    class SingleToken(val token: String) : ParsedTokens {
        override fun toString(): String = token
    }
}

sealed interface SyntaxError {
    object UnexpectedEos : SyntaxError
    class UnexpectedToken(val token: String) : SyntaxError
}

/**
 * Intermediate state of interpretation.
 *
 * The properties are expected to change during tokenizing and parsing.
 */
class ParseState(
    /** Raw input text as LISP code */
    var lispText: String,
    /** Index representing a token position to be parsed */
    var index: Int,
)

/**
 * [tokens] or [error] is non-null exclusively.
 */
class ParseResult(val tokens: ParsedTokens?, val error: SyntaxError?) {
    override fun toString(): String = tokens?.toString() ?: error?.toString() ?: ""
}
