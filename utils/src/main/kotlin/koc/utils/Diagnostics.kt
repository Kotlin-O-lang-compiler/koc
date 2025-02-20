package koc.utils

class Diagnostics {
    private val errors = mutableListOf<CompileException>()

    val hasErrors get() = errors.isNotEmpty()

    fun error(error: CompileException, position: Position) {
        errors.add(error)
        printError(error, position)
    }

    private fun printError(error: CompileException, position: Position) {
        System.err.println("error: $error")
        System.err.println(">>> ${position.toVerboseString()}")
    }

    fun clear() {
        errors.clear()
    }
}