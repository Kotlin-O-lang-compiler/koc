package koc.ast.visitor

import koc.ast.Argument
import koc.ast.Assignment
import koc.ast.Body
import koc.ast.BooleanLiteral
import koc.ast.CallExpr
import koc.ast.ClassBody
import koc.ast.ClassDecl
import koc.ast.ConstructorDecl
import koc.ast.FieldDecl
import koc.ast.File
import koc.ast.GenericParams
import koc.ast.IfNode
import koc.ast.IntegerLiteral
import koc.ast.InvalidExpr
import koc.ast.MemberAccessExpr
import koc.ast.MethodBody
import koc.ast.MethodDecl
import koc.ast.Param
import koc.ast.Params
import koc.ast.RealLiteral
import koc.ast.RefExpr
import koc.ast.ReturnNode
import koc.ast.TypeParam
import koc.ast.TypeRef
import koc.ast.VarDecl
import koc.ast.WhileNode

abstract class AbstractVoidVisitor(
    order: Order = Order.TOP_DOWN, onBroken: Insight = Insight.SKIP
) : AbstractVisitor<Unit>(order, onBroken) {
    override fun visit(classDecl: ClassDecl) {}
    override fun visit(body: ClassBody) {}
    override fun visit(vardecl: VarDecl) {}
    override fun visit(field: FieldDecl) {}
    override fun visit(method: MethodDecl) {}
    override fun visit(ctor: ConstructorDecl) {}
    override fun visit(lit: IntegerLiteral) {}
    override fun visit(lit: RealLiteral) {}
    override fun visit(lit: BooleanLiteral) {}
    override fun visit(expr: RefExpr) {}
    override fun visit(expr: CallExpr) {}
    override fun visit(expr: InvalidExpr) {}
    override fun visit(expr: MemberAccessExpr) {}
    override fun visit(node: WhileNode) {}
    override fun visit(node: IfNode) {}
    override fun visit(node: Assignment) {}
    override fun visit(node: ReturnNode) {}
    override fun visit(node: Argument) {}
    override fun visit(node: Body) {}
    override fun visit(node: File) {}
    override fun visit(node: GenericParams) {}
    override fun visit(node: MethodBody) {}
    override fun visit(node: Param) {}
    override fun visit(node: Params) {}
    override fun visit(node: TypeParam) {}
    override fun visit(ref: TypeRef) {}
}
