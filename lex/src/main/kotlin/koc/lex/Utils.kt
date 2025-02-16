package koc.lex

import java.io.PrintStream

/**
 * Syntactical check for identifier
 */
internal val StringBuilder.isIdentifier: Boolean
    get() {
        if (isEmpty()) return false
        val first = first()
        if (!first.isLetter() && first != '_') return false

        for (i in 1 ..< length) {
            if (!this[i].isLetterOrDigit() && this[i] != '_') return false
        }
        return true
    }

/**
 * Syntactical check for integer
 */
internal val StringBuilder.isInteger: Boolean
    get() {
        if (isEmpty()) return false

        if (!first().isDigit() && first() !in listOf('+', '-')) return false

        for (i in 1 ..< length) {
            if (!this[i].isDigit()) return false
        }

        return true
    }

/**
 * Syntactical check for real
 */
internal val StringBuilder.isReal: Boolean
    get() {
        if (isEmpty()) return false

        if (!first().isDigit() && first() !in listOf('+', '-')) return false
        var hadSeparator = false // Was point met

        for (i in 1 ..< length) {
            if (this[i] == '.') {
                if (hadSeparator) return false
                hadSeparator = true
            }

            if (!this[i].isDigit() || this[i] != '.') return false
        }

        return true
    }


fun Collection<Token>.dump(out: PrintStream = System.out) {
    var line = 1u
    var column = 1u

    for (token in this) {
        if (line < token.start.line) {
            out.print("\n".repeat((token.start.line - line).toInt()))
            line = token.start.line
        }

        if (column < token.start.column) {
            out.println(" ".repeat((token.start.column - column).toInt()))
            column = token.start.column
        }

        out.print("${token.kind}(${token.value})")
        line = token.end.line
        column = token.end.column
    }
}