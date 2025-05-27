package koc.core

open class CompileException(override val message: String, override val cause: Throwable? = null) : Exception(message, cause) {
    override fun toString(): String {
        if (cause == null)
            return message

        return super.toString()
    }
}