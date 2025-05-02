package koc.parser

import koc.lex.Token
import koc.parser.ast.Assignment
import koc.parser.ast.BooleanLiteral
import koc.parser.ast.ClassBody
import koc.parser.ast.ClassDecl
import koc.parser.ast.ClassMemberDecl
import koc.parser.ast.ConstructorDecl
import koc.parser.ast.Expr
import koc.parser.ast.IfNode
import koc.parser.ast.IntegerLiteral
import koc.parser.ast.MethodDecl
import koc.parser.ast.Node
import koc.parser.ast.RealLiteral
import koc.parser.ast.ReturnNode
import koc.parser.ast.ThisExpr
import koc.parser.ast.VarDecl
import koc.parser.ast.WhileNode

interface Parser {
    fun parse(tokens: List<Token>): Node

    fun parseClassDecl(tokens: List<Token>): ClassDecl
    fun parseClassBody(tokens: List<Token>): ClassBody
    fun parseClassMemberDecl(tokens: List<Token>): ClassMemberDecl

    fun parseVarDecl(tokens: List<Token>): VarDecl
    fun parseMethod(tokens: List<Token>): MethodDecl
    fun parseConstructor(tokens: List<Token>): ConstructorDecl

    fun parseExpr(tokens: List<Token>): Expr
    fun parseIntegerLiteral(tokens: List<Token>): IntegerLiteral
    fun parseRealLiteral(tokens: List<Token>): RealLiteral
    fun parseBooleanLiteral(tokens: List<Token>): BooleanLiteral
    fun parseThisExpr(tokens: List<Token>): ThisExpr

    fun parseWhileLoop(tokens: List<Token>): WhileNode
    fun parseIfNode(tokens: List<Token>): IfNode
    fun parseAssignment(tokens: List<Token>): Assignment
    fun parseReturnNode(tokens: List<Token>): ReturnNode
}