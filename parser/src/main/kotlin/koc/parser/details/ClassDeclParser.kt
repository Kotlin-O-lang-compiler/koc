package koc.parser.details

import koc.lex.Token
import koc.parser.ast.ClassDecl

interface ClassDeclParser {
    fun parseClassDecl(tokens: List<Token>): ClassDecl
}