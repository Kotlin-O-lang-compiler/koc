package koc.parser

import koc.lex.Token
import koc.ast.Node
import koc.parser.impl.ParserImpl
import koc.core.Diagnostics

fun parse(
    source: List<Token>,
    diag: Diagnostics// = Diagnostics()
): List<Node> {
    val parser = Parser.fromOptions(diag = diag) as ParserImpl
    return parser.parseNodes(source)
}
