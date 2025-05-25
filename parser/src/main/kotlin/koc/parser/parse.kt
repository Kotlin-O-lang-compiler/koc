package koc.parser

import koc.ast.Node
import koc.core.Diagnostics
import koc.lex.Tokens
import koc.parser.impl.ParserImpl

fun parse(
    source: Tokens,
    diag: Diagnostics
): List<Node> {
    val parser = Parser.fromOptions(diag = diag) as ParserImpl
    return parser.parseNodes(source)
}
