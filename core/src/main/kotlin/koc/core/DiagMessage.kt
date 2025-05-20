package koc.core

abstract class DiagMessage(val kind: DiagKind) {
    abstract override fun toString(): String

    val verbosity: DiagKind.Verbosity
        get() = kind.verbosity

    open val extraMessage: String? = null
}