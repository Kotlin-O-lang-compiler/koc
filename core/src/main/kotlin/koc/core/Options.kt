package koc.core

data class KocOptions(
    val dumpTokens: Boolean = false,
    val dumpParse: Boolean = false,
    /**
     * Eager stop
     */
    val stopOnError: Boolean = false,
)
