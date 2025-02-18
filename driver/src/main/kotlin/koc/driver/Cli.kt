package koc.driver

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.help
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.path
import java.nio.file.Path
import koc.driver.api.koc
import koc.utils.Diagnostics

class Compiler : CliktCommand(help = "Options for running koc") {
    private val sourceFiles: List<Path> by argument(name = "sources")
        .path(mustExist = true, canBeFile = true, canBeDir = false)
        .multiple(required = true)
        .help("The path to the O-lang source files")

    private val dumpTokens by option("--dump-tokens")
        .flag(default = false)

    private val stopOnError by option("--stop-on-error")
        .flag(default = false)

    override fun run() {
        val diagnostics = Diagnostics()
        val options = KocOptions(
            dumpTokens = dumpTokens,
            stopOnError = stopOnError,
        )

        koc(sourceFiles, diagnostics, options)
    }
}

fun main(args: Array<String>) = Compiler().main(args)
