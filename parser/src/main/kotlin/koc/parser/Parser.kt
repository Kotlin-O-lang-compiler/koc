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
import koc.ast.TypeRef
import koc.ast.VarDecl
import koc.ast.WhileNode
import koc.lex.Tokens

interface Parser {
    fun parseNodes(tokens: Tokens): List<Node>
//    fun parse(tokens: Tokens): Node

    fun parseClassDecl(tokens: Tokens): ClassDecl
    fun parseClassBody(tokens: Tokens): ClassBody
    fun parseClassMemberDecl(tokens: Tokens): ClassMemberDecl

    fun parseVarDecl(tokens: Tokens): VarDecl
    fun parseMethod(tokens: Tokens): MethodDecl
    fun parseConstructor(tokens: Tokens): ConstructorDecl

    fun parseExpr(tokens: Tokens): Expr
    fun parseIntegerLiteral(tokens: Tokens): IntegerLiteral
    fun parseRealLiteral(tokens: Tokens): RealLiteral
    fun parseBooleanLiteral(tokens: Tokens): BooleanLiteral
    fun parseRefExpr(tokens: Tokens): RefExpr

    fun parseTypeRef(tokens: Tokens): TypeRef

    fun parseWhileLoop(tokens: Tokens): WhileNode
    fun parseIfNode(tokens: Tokens): IfNode
    fun parseAssignment(tokens: Tokens): Assignment
    fun parseReturnNode(tokens: Tokens): ReturnNode

    companion object
}