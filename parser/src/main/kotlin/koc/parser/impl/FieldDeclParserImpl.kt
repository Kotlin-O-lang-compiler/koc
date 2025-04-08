package koc.parser.impl

import koc.lex.Token
import koc.parser.ast.FieldDecl
import koc.parser.details.FieldDeclParser
import koc.utils.Diagnostics

class FieldDeclParserImpl(
    val diag: Diagnostics
) : FieldDeclParser {
    override fun parseFieldDecl(tokens: List<Token>): FieldDecl {

    }
}