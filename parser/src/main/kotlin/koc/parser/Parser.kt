package koc.parser

import koc.lex.Token
import koc.ast.Assignment
import koc.ast.BooleanLiteral
import koc.ast.ClassBody
import koc.ast.ClassDecl
import koc.ast.ClassMemberDecl
import koc.ast.ConstructorDecl
import koc.ast.Expr
import koc.ast.IfNode
import koc.ast.IntegerLiteral
import koc.ast.MethodDecl
import koc.ast.Node
import koc.ast.RealLiteral
import koc.ast.RefExpr
import koc.ast.ReturnNode
import koc.ast.VarDecl
import koc.ast.WhileNode

interface Parser {
    fun parseNodes(tokens: List<Token>): List<Node>
//    fun parse(tokens: List<Token>): Node

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
    fun parseRefExpr(tokens: List<Token>): RefExpr

    fun parseWhileLoop(tokens: List<Token>): WhileNode
    fun parseIfNode(tokens: List<Token>): IfNode
    fun parseAssignment(tokens: List<Token>): Assignment
    fun parseReturnNode(tokens: List<Token>): ReturnNode

    companion object
}