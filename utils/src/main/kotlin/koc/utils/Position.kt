package koc.utils

data class Position(val line: UInt, val column: UInt, val filename: String) {
    override fun toString(): String = "$line:$column"

    fun toVerboseString(): String = "$filename:${toString()}"
}