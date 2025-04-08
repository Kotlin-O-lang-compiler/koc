package koc.parser

import koc.lex.Token
import koc.parser.ast.ClassDecl
import koc.parser.ast.ClassMemberDecl
import koc.parser.ast.Node
import koc.parser.details.ClassDeclParser

interface Parser : ClassDeclParser {
    fun parse(tokens: List<Token>): Node

    fun parseClassMemberDecl(tokens: List<Token>): ClassMemberDecl

}