package koc.utils

data class Position(val line: UInt, val column: UInt) {
    override fun toString(): String = "$line:$column"
}