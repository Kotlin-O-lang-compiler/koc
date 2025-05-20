package koc.driver

import koc.driver.api.dumpParse
import koc.driver.api.dumpTokens
import koc.lex.Lexer
import koc.lex.Token
import koc.lex.fromOptions
import koc.parser.Parser
import koc.ast.Node
import koc.parser.fromOptions
import koc.parser.parse
import koc.sema.TypeManager
import koc.sema.semaStages
import koc.core.Diagnostics
import koc.core.KocOptions
import java.nio.file.Path
import kotlin.io.path.name

class Kocpiler private constructor(
    val diag: Diagnostics = Diagnostics(),
    val options: KocOptions = KocOptions(),
    initialLexer: Lexer? = null,
    initialParser: Parser? = null,
    initialTypeManager: TypeManager? = null,
    marker: Unit
) {
    constructor(
        diag: Diagnostics = Diagnostics(),
        opts: KocOptions = KocOptions(),
        lexer: Lexer = Lexer.fromOptions(opts, diag)
    ) : this(diag, opts, lexer, null, null, Unit)

    constructor(
        diag: Diagnostics = Diagnostics(),
        opts: KocOptions = KocOptions(),
        lexer: Lexer = Lexer.fromOptions(opts, diag),
        parser: Parser = Parser.fromOptions(opts, diag)
    ) : this(diag, opts, lexer, parser, null, Unit)

    constructor(
        diag: Diagnostics = Diagnostics(),
        opts: KocOptions = KocOptions(),
        lexer: Lexer = Lexer.fromOptions(opts, diag),
        parser: Parser = Parser.fromOptions(opts, diag),
        typeManager: TypeManager = TypeManager(lexer, parser),
    ) : this(diag, opts, lexer, parser, typeManager, Unit)

    val lexer: Lexer by lazy { initialLexer ?: Lexer.fromOptions(options, diag) }
    val parser: Parser by lazy { initialParser ?: Parser.fromOptions(options, diag) }
    val typeManager: TypeManager by lazy { initialTypeManager ?: TypeManager(lexer, parser) }

    val semaStages: List<(List<Node>) -> Unit>
        get() = semaStages(typeManager, diag)

    fun run(program: Path) {
        if (diag.hasErrors && options.stopOnError) return
        val tokens = lexer.lex(program.toFile())
        run(tokens, program.name)
    }

    fun run(program: String, name: String = "program") {
        if (diag.hasErrors && options.stopOnError) return
        val tokens = lexer.lex(program, name)
        run(tokens, name)
    }

    private fun run(tokens: List<Token>, programName: String) {
        if (options.dumpTokens) {
            dumpTokens(tokens, programName, options)
            return
        }

        val nodes = parse(tokens, diag)

        if (options.dumpParse) {
            dumpParse(nodes, programName, options)
            return
        }

        semaStages.forEach { it(nodes) }

        // TODO("codegen is not implemented yet")
    }
}
