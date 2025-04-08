package koc.parser.impl

import koc.lex.Token
import koc.parser.ast.ClassDecl
import koc.parser.details.ClassDeclParser
import koc.utils.Diagnostics

class ClassDeclParserImpl(
    val diag: Diagnostics
) : ClassDeclParser {
    override fun parseClassDecl(tokens: List<Token>): ClassDecl {
        TODO()
    }
}