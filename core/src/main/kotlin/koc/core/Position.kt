package koc.core

data class Position(val line: UInt, val column: UInt, val filename: String) : Comparable<Position> {
    constructor(line: UInt, column: UInt) : this(line, column, "_unnamed")
    override fun toString(): String = "$line:$column"

    fun toVerboseString(): String = "$filename:${toString()}"

    companion object {
        val fake = Position(0u, 0u, "")
    }

    override fun compareTo(other: Position): Int = when (val lineRes = line.compareTo(other.line)) {
        0 -> column.compareTo(other.column)
        else -> lineRes
    }

    infix operator fun plus(other: Position): Position {
        return Position(this.line + other.line, this.column + other.column, this.filename)
    }

    fun plus(lines: UInt = 0u, columns: UInt = 0u): Position {
        return Position(this.line + lines, this.column + columns, this.filename)
    }

    fun minus(lines: UInt = 0u, columns: UInt = 0u): Position {
        return Position(this.line - lines, this.column - columns, this.filename)
    }
}