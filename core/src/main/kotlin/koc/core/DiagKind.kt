package koc.core

abstract class DiagKind(val verbosity: Verbosity) {
    val kind = this::class.simpleName!!
    override fun hashCode(): Int = kind.hashCode()
    override fun equals(other: Any?): Boolean = kind == other
    override fun toString(): String = kind

    enum class Verbosity {
        INFO, WARNING, ERROR;

        fun asString(): String = this.toString().lowercase()
    }
}