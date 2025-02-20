package koc.lex

import java.io.PrintStream


internal val Char.isIdentifier: Boolean
    get() = isLetterOrDigit() || this == '_'

internal val Char.isIdentifierStart: Boolean
    get() = isLetter() || this == '_'

/**
 * Syntactical check for identifier
 */
internal val StringBuilder.isIdentifier: Boolean
    get() {
        if (isEmpty()) return false
        val first = first()
        if (!first.isIdentifierStart) return false

        for (i in 1 ..< length) {
            if (!this[i].isIdentifier) return false
        }
        return true
    }

internal val Char.isIntegerStart: Boolean
    get() = isDigit() || this == '-' || this == '+'

internal val Char.isInteger: Boolean
    get() = isDigit()

/**
 * Syntactical check for integer
 */
internal val StringBuilder.isInteger: Boolean
    get() {
        if (isEmpty()) return false

        if (!first().isIntegerStart) return false

        for (i in 1 ..< length) {
            if (!this[i].isInteger) return false
        }

        return true
    }

internal val Char.isRealStart: Boolean
    get() = isDigit() || this == '-' || this == '+'

internal val Char.isReal: Boolean
    get() = isDigit() || this == '.'

/**
 * Syntactical check for real
 */
internal val StringBuilder.isReal: Boolean
    get() {
        if (isEmpty()) return false

        if (!first().isRealStart) return false
        var hadSeparator = false // Was point met

        for (i in 1 ..< length) {
            if (this[i] == '.') {
                if (hadSeparator) return false
                hadSeparator = true
            }

            if (!this[i].isReal) return false
        }

        return true
    }


fun Collection<Token>.dump(out: PrintStream = System.out) {
    var line = 1u
    var column = 1u

    for (token in this) {
        if (line < token.start.line) {
            out.print("\n".repeat((token.start.line - line).toInt()))
            column = 1u
        }

        if (column < token.start.column) {
            out.print(" ".repeat((token.start.column - column).toInt()))
        }

        out.print("${token.kind}(${token.value})")
        line = token.end.line
        column = token.end.column
    }
}