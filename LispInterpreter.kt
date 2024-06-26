package com.example.lispinterpreter

fun main() {
    // Example of usage of the interpreter.
    val interpreter = LispInterpreter()
    interpreter.interpret("12")
    interpreter.interpret("()")
    interpreter.interpret("((1 2) ((2) 3))")
}

/**
 * Tokenize list string from a LISP code by reverse order.
 */
class LispTokenizer (val lispText: String) {
    fun tokenize(): List<String> {
        val tokens: MutableList<String> = mutableListOf()
        var startIndex = 0
        var endIndex = 0

        while (endIndex < lispText.length) {
            val char = lispText[endIndex]
            // Skip single token because it is already token
            if (!Symbols.singleCharToken(char)) {
                endIndex += 1
                continue
            }

            if (startIndex < endIndex) {
                tokens += lispText.substring(startIndex, endIndex)
            }
            if (char != Symbols.SPACE) {
                tokens += char.toString()
            }
            startIndex = endIndex + 1
            endIndex += 1
        }

        if (startIndex < lispText.length) {
            tokens += lispText.substring(startIndex)
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

    fun interpret(lispText: String) {
        val tokens = LispTokenizer(lispText).tokenize()
        val parseResult = LispParser().parse(tokens)
        // TODO: Implement `evaluation` logic. Currently, just show the parse result.
        println(parseResult)
    }
}

/**
 * Read and parse Lisp token recursive and create Abstract Syntac Tree.
 */
class LispParser {
    var index = 0
    fun parse(tokens: List<String>): ParseResult {
        if (this.index >= tokens.size) {
            return SyntaxError.UnexpectedEos
        }
        val tokenString = tokens[this.index]
        if (tokenString == Symbols.S_CLOSE) {
            return SyntaxError.UnexpectedToken(tokenString)
        }

        return when {
            tokenString != Symbols.S_OPEN -> {
                this.index += 1
                ParsedTokens.SingleToken(tokenString)
            }

            tokens[this.index + 1] == Symbols.S_CLOSE -> {
                this.index += 2
                ParsedTokens.TokenGroup(listOf())
            }

            else -> {
                this.index += 1
                val parseResult = parse(tokens)
                when(parseResult) {
                    is ParsedTokens -> return parseTokensInParentheses(parseResult, tokens)
                    is SyntaxError -> return parseResult
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
                return SyntaxError.UnexpectedEos
            }

            this.index = indexInParentheses
            val recursiveResult = parse(tokens0)
            when (recursiveResult) {
                is SyntaxError -> return recursiveResult
                is ParsedTokens -> {
                    indexInParentheses = this.index
                    tokensInParentheses.add(recursiveResult)
                }
            }

        }
        this.index = indexInParentheses + 1
        return ParsedTokens.TokenGroup(tokensInParentheses)
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
sealed interface ParsedTokens : ParseResult{
    class TokenGroup(val tokens: List<ParsedTokens>) : ParsedTokens {
        override fun toString(): String =
            tokens.joinToString(separator = ", ", prefix = "[", postfix = "]")
    }

    class SingleToken(val token: String) : ParsedTokens {
        override fun toString(): String = token
    }
}

sealed interface SyntaxError : ParseResult {
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
sealed interface ParseResult {
}
