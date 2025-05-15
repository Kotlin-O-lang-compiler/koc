package koc.parser.ast

@JvmInline
value class Identifier(val value: String) {
    override fun toString(): String {
        return value
    }
}