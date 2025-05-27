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

abstract class AbstractVoidInsightVisitor(
    order: Order = Order.TOP_DOWN, onBroken: Insight = Insight.SKIP
) : AbstractVisitor<Insight>(order, onBroken) {
    override fun visit(classDecl: ClassDecl) = insight
    override fun visit(body: ClassBody) = insight
    override fun visit(vardecl: VarDecl) = insight
    override fun visit(field: FieldDecl) = insight
    override fun visit(method: MethodDecl) = insight
    override fun visit(ctor: ConstructorDecl) = insight
    override fun visit(lit: IntegerLiteral) = insight
    override fun visit(lit: RealLiteral) = insight
    override fun visit(lit: BooleanLiteral) = insight
    override fun visit(expr: RefExpr) = insight
    override fun visit(expr: CallExpr) = insight
    override fun visit(expr: InvalidExpr) = insight
    override fun visit(expr: MemberAccessExpr) = insight
    override fun visit(node: WhileNode) = insight
    override fun visit(node: IfNode) = insight
    override fun visit(node: Assignment) = insight
    override fun visit(node: ReturnNode) = insight
    override fun visit(node: Argument) = insight
    override fun visit(node: Body) = insight
    override fun visit(node: File) = insight
    override fun visit(node: GenericParams) = insight
    override fun visit(node: MethodBody) = insight
    override fun visit(node: Param) = insight
    override fun visit(node: Params) = insight
    override fun visit(node: TypeParam) = insight
    override fun visit(ref: TypeRef) = insight
}
