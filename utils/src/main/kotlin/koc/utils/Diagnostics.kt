package koc.utils

class Diagnostics {
    private val errors = mutableListOf<CompileException>()

    val hasErrors get() = errors.isNotEmpty()// || dumpedErrors.isNotEmpty()

    fun error(error: CompileException) {
        errors.add(error)
        System.err.println(error)
    }

    fun clear() {
        errors.clear()
    }
}