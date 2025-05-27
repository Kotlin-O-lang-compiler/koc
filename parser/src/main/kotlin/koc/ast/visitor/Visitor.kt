package koc.ast.visitor

import koc.ast.Argument
import koc.ast.Assignment
import koc.ast.Body
import koc.ast.BooleanLiteral
import koc.ast.CallExpr
import koc.ast.ClassBody
import koc.ast.ClassDecl
import koc.ast.ClassMemberDecl
import koc.ast.ConstructorDecl
import koc.ast.Expr
import koc.ast.FieldDecl
import koc.ast.File
import koc.ast.GenericParams
import koc.ast.IfNode
import koc.ast.IntegerLiteral
import koc.ast.InvalidExpr
import koc.ast.MemberAccessExpr
import koc.ast.MethodBody
import koc.ast.MethodDecl
import koc.ast.Node
import koc.ast.Param
import koc.ast.Params
import koc.ast.RealLiteral
import koc.ast.RefExpr
import koc.ast.ReturnNode
import koc.ast.Statement
import koc.ast.TypeParam
import koc.ast.TypeRef
import koc.ast.VarDecl
import koc.ast.WhileNode

interface Visitor <T> {
    val insight: Insight
    val order: Order
    val onBroken: Insight

    fun previsit(node: Node) = when (node) {
        is ClassDecl -> previsit(node)
        is ClassBody -> previsit(node)
        is ClassMemberDecl -> previsit(node)
        is Expr -> previsit(node)
        is Statement -> previsit(node)
        is Argument -> previsit(node)
        is VarDecl -> previsit(node)
        is Body -> previsit(node)
        is File -> previsit(node)
        is GenericParams -> previsit(node)
        is MethodBody -> previsit(node)
        is Param -> previsit(node)
        is Params -> previsit(node)
        is TypeParam -> previsit(node)
        is TypeRef -> previsit(node)
    }
    fun visit(node: Node): T = when (node) {
        is ClassDecl -> visit(node)
        is ClassBody -> visit(node)
        is ClassMemberDecl -> visit(node)
        is Expr -> visit(node)
        is Statement -> visit(node)
        is Argument -> visit(node)
        is VarDecl -> visit(node)
        is Body -> visit(node)
        is File -> visit(node)
        is GenericParams -> visit(node)
        is MethodBody -> visit(node)
        is Param -> visit(node)
        is Params -> visit(node)
        is TypeParam -> visit(node)
        is TypeRef -> visit(node)
    }
    fun postvisit(node: Node, res: T) = when (node) {
        is ClassDecl -> postvisit(node, res)
        is ClassBody -> postvisit(node, res)
        is ClassMemberDecl -> postvisit(node, res)
        is Expr -> postvisit(node, res)
        is Statement -> postvisit(node, res)
        is Argument -> postvisit(node, res)
        is VarDecl -> postvisit(node, res)
        is Body -> postvisit(node, res)
        is File -> postvisit(node, res)
        is GenericParams -> postvisit(node, res)
        is MethodBody -> postvisit(node, res)
        is Param -> postvisit(node, res)
        is Params -> postvisit(node, res)
        is TypeParam -> postvisit(node, res)
        is TypeRef -> postvisit(node, res)
    }

    fun previsit(classDecl: ClassDecl) {}
    fun visit(classDecl: ClassDecl): T
    fun postvisit(classDecl: ClassDecl, res: T) {}

    fun previsit(body: ClassBody) {}
    fun visit(body: ClassBody): T
    fun postvisit(body: ClassBody, res: T) {}

    fun previsit(member: ClassMemberDecl) = when (member) {
        is FieldDecl -> previsit(member)
        is MethodDecl -> previsit(member)
        is ConstructorDecl -> previsit(member)
    }
    fun visit(member: ClassMemberDecl): T = when (member) {
        is FieldDecl -> visit(member)
        is MethodDecl -> visit(member)
        is ConstructorDecl -> visit(member)
    }
    fun postvisit(member: ClassMemberDecl, res: T) = when (member) {
        is FieldDecl -> postvisit(member, res)
        is MethodDecl -> postvisit(member, res)
        is ConstructorDecl -> postvisit(member, res)
    }

    fun previsit(vardecl: VarDecl) {}
    fun visit(vardecl: VarDecl): T
    fun postvisit(vardecl: VarDecl, res: T) {}

    fun previsit(field: FieldDecl) {}
    fun visit(field: FieldDecl): T
    fun postvisit(field: FieldDecl, res: T) {}

    fun previsit(method: MethodDecl) {}
    fun visit(method: MethodDecl): T
    fun postvisit(method: MethodDecl, res: T) {}

    fun previsit(ctor: ConstructorDecl) {}
    fun visit(ctor: ConstructorDecl): T
    fun postvisit(ctor: ConstructorDecl, res: T) {}


    fun previsit(expr: Expr) = when (expr) {
        is IntegerLiteral -> previsit(expr)
        is RealLiteral -> previsit(expr)
        is BooleanLiteral -> previsit(expr)
        is RefExpr -> previsit(expr)
        is CallExpr -> previsit(expr)
        is InvalidExpr -> previsit(expr)
        is MemberAccessExpr -> previsit(expr)
    }
    fun visit(expr: Expr): T = when (expr) {
        is IntegerLiteral -> visit(expr)
        is RealLiteral -> visit(expr)
        is BooleanLiteral -> visit(expr)
        is RefExpr -> visit(expr)
        is CallExpr -> visit(expr)
        is InvalidExpr -> visit(expr)
        is MemberAccessExpr -> visit(expr)
    }
    fun postvisit(expr: Expr, res: T) = when (expr) {
        is IntegerLiteral -> postvisit(expr, res)
        is RealLiteral -> postvisit(expr, res)
        is BooleanLiteral -> postvisit(expr, res)
        is RefExpr -> postvisit(expr, res)
        is CallExpr -> postvisit(expr, res)
        is InvalidExpr -> postvisit(expr, res)
        is MemberAccessExpr -> postvisit(expr, res)
    }

    fun previsit(lit: IntegerLiteral) {}
    fun visit(lit: IntegerLiteral): T
    fun postvisit(lit: IntegerLiteral, res: T) {}

    fun previsit(lit: RealLiteral) {}
    fun visit(lit: RealLiteral): T
    fun postvisit(lit: RealLiteral, res: T) {}

    fun previsit(lit: BooleanLiteral) {}
    fun visit(lit: BooleanLiteral): T
    fun postvisit(lit: BooleanLiteral, res: T) {}

    fun previsit(expr: RefExpr) {}
    fun visit(expr: RefExpr): T
    fun postvisit(expr: RefExpr, res: T) {}

    fun previsit(expr: CallExpr) {}
    fun visit(expr: CallExpr): T
    fun postvisit(expr: CallExpr, res: T) {}

    fun previsit(expr: InvalidExpr) {}
    fun visit(expr: InvalidExpr): T
    fun postvisit(expr: InvalidExpr, res: T) {}

    fun previsit(expr: MemberAccessExpr) {}
    fun visit(expr: MemberAccessExpr): T
    fun postvisit(expr: MemberAccessExpr, res: T) {}

    fun previsit(node: Statement) = when (node) {
        is WhileNode -> previsit(node)
        is IfNode -> previsit(node)
        is Assignment -> previsit(node)
        is ReturnNode -> previsit(node)
    }
    fun visit(node: Statement): T = when (node) {
        is WhileNode -> visit(node)
        is IfNode -> visit(node)
        is Assignment -> visit(node)
        is ReturnNode -> visit(node)
    }
    fun postvisit(node: Statement, res: T) = when (node) {
        is WhileNode -> postvisit(node, res)
        is IfNode -> postvisit(node, res)
        is Assignment -> postvisit(node, res)
        is ReturnNode -> postvisit(node, res)
    }

    fun previsit(node: WhileNode) {}
    fun visit(node: WhileNode): T
    fun postvisit(node: WhileNode, res: T) {}

    fun previsit(node: IfNode) {}
    fun visit(node: IfNode): T
    fun postvisit(node: IfNode, res: T) {}

    fun previsit(node: Assignment) {}
    fun visit(node: Assignment): T
    fun postvisit(node: Assignment, res: T) {}

    fun previsit(node: ReturnNode) {}
    fun visit(node: ReturnNode): T
    fun postvisit(node: ReturnNode, res: T) {}

    fun previsit(node: Argument) {}
    fun visit(node: Argument): T
    fun postvisit(node: Argument, res: T) {}

    fun previsit(node: Body) {}
    fun visit(node: Body): T
    fun postvisit(node: Body, res: T) {}

    fun previsit(node: File) {}
    fun visit(node: File): T
    fun postvisit(node: File, res: T) {}

    fun previsit(node: GenericParams) {}
    fun visit(node: GenericParams): T
    fun postvisit(node: GenericParams, res: T) {}

    fun previsit(node: MethodBody) {}
    fun visit(node: MethodBody): T
    fun postvisit(node: MethodBody, res: T) {}

    fun previsit(node: Param) {}
    fun visit(node: Param): T
    fun postvisit(node: Param, res: T) {}

    fun previsit(node: Params) {}
    fun visit(node: Params): T
    fun postvisit(node: Params, res: T) {}

    fun previsit(node: TypeParam) {}
    fun visit(node: TypeParam): T
    fun postvisit(node: TypeParam, res: T) {}

    fun previsit(ref: TypeRef) {}
    fun visit(ref: TypeRef): T
    fun postvisit(ref: TypeRef, res: T) {}

    val shouldVisitChildren: Boolean
        get() = insight != Insight.STOP && insight != Insight.SKIP

    fun stop()
    fun reset()
}