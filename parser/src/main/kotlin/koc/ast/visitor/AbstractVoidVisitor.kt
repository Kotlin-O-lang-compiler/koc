package koc.parser.ast.visitor

import koc.parser.ast.Argument
import koc.parser.ast.Assignment
import koc.parser.ast.Body
import koc.parser.ast.BooleanLiteral
import koc.parser.ast.CallExpr
import koc.parser.ast.ClassBody
import koc.parser.ast.ClassDecl
import koc.parser.ast.ConstructorDecl
import koc.parser.ast.FieldDecl
import koc.parser.ast.File
import koc.parser.ast.GenericParams
import koc.parser.ast.IfNode
import koc.parser.ast.IntegerLiteral
import koc.parser.ast.InvalidExpr
import koc.parser.ast.MemberAccessExpr
import koc.parser.ast.MethodBody
import koc.parser.ast.MethodDecl
import koc.parser.ast.Param
import koc.parser.ast.Params
import koc.parser.ast.RealLiteral
import koc.parser.ast.RefExpr
import koc.parser.ast.ReturnNode
import koc.parser.ast.TypeParam
import koc.parser.ast.VarDecl
import koc.parser.ast.WhileNode

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
}
