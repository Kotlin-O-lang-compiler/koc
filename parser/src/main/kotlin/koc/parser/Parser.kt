package koc.parser

import koc.lex.Token
import koc.parser.ast.BooleanLiteral
import koc.parser.ast.ClassDecl
import koc.parser.ast.ClassMemberDecl
import koc.parser.ast.Expr
import koc.parser.ast.IntegerLiteral
import koc.parser.ast.Node
import koc.parser.ast.RealLiteral
import koc.parser.ast.ThisExpr
import koc.parser.ast.VarDecl

interface Parser {
    fun parse(tokens: List<Token>): Node

    fun parseClassDecl(tokens: List<Token>): ClassDecl
    fun parseClassMemberDecl(tokens: List<Token>): ClassMemberDecl

    fun parseVarDecl(tokens: List<Token>): VarDecl

    fun parseExpr(tokens: List<Token>): Expr
//    fun parsePrimaryExpr(tokens: List<Token>): Expr
    fun parseIntegerLiteral(tokens: List<Token>): IntegerLiteral
    fun parseRealLiteral(tokens: List<Token>): RealLiteral
    fun parseBooleanLiteral(tokens: List<Token>): BooleanLiteral
    fun parseThisExpr(tokens: List<Token>): ThisExpr
}