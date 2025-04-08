package koc.parser.details

import koc.lex.Token
import koc.parser.ast.ClassDecl
import koc.parser.ast.FieldDecl

interface FieldDeclParser {
    fun parseFieldDecl(tokens: List<Token>): FieldDecl
}