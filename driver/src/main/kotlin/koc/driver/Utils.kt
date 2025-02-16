package koc.driver

import koc.utils.Diagnostics
import java.io.File

data class CommandLineParseResult(val programFiles: List<File>, val arguments: Set<Arguments>)

internal fun parse(args: Array<String>, diag: Diagnostics): CommandLineParseResult {
    val arguments = mutableSetOf<Arguments>()
    val programs = arrayListOf<File>()

    for (arg in args) {

        val parsedArg: Arguments? = if (arg.startsWith("-")) {
            when {
                arg == Arguments.DumpTokens.name -> Arguments.DumpTokens
                else -> { diag.error(UnknownArgumentException(arg)); null }
            }
        } else {
            programs += File(arg)
            null
        }

        parsedArg?.let { arguments += it }
    }

    if (programs.isEmpty()) diag.error(ProgramsNotPassedException())

    return CommandLineParseResult(programs, arguments)
}