package koc.sema.impl

import koc.ast.ClassDecl
import koc.ast.ClassType
import koc.ast.visitor.AbstractVoidInsightVisitor
import koc.ast.visitor.Insight
import koc.core.Diagnostics
import koc.lex.diag
import koc.parser.ast.Attribute
import koc.parser.ast.Identifier
import koc.sema.TypeManager
import koc.sema.diag.RecursiveInheritance
import koc.sema.diag.UndefinedReference

class SuperTypeResolver(
    private val typeManager: TypeManager,
    private val diag: Diagnostics
) : AbstractVoidInsightVisitor() {
    private val inheritanceChain = arrayListOf<Identifier>()

    override fun visit(classDecl: ClassDecl) = resolve(classDecl) {
        if (typeManager.hasType(classDecl.identifier)) return@resolve Insight.SKIP

        val superType = classDecl.superTypeRef?.let {
            if (typeManager.hasType(it.identifier.value)) typeManager.getType(it.identifier.value)
            else if (typeManager.hasDefinition(it.identifier)) {
                typeManager.getDefinition(it.identifier).visit(this)
                typeManager.getType(it.identifier)
            } else {
                classDecl.enable(Attribute.BROKEN)
                diag.diag(UndefinedReference(it), it.window)
                it.specifyRef(typeManager.invalidDecl)
                it.specifyType(typeManager.invalidType)
                typeManager.invalidType
            }
        } ?: typeManager.classType

        val type = ClassType(classDecl, superType)
        typeManager.learn(type)
        return@resolve Insight.SKIP
    }

    private fun resolve(node: ClassDecl, withNode: (ClassDecl) -> Insight): Insight {
        inheritanceChain += node.identifier
        if (node.inTypeCheck) {
            node.enable(Attribute.BROKEN)
            diag.diag(
                RecursiveInheritance(node, inheritanceChain.toList()),
                node.identifierToken.start,
                node.superTypeRef?.end ?: node.identifierToken.end
            )
            inheritanceChain.removeLast()
            return Insight.SKIP
        }
        node.enable(Attribute.IN_TYPE_CHECK)
        val res = withNode(node)
        node.disable(Attribute.IN_TYPE_CHECK)
        inheritanceChain.removeLast()
        return res
    }
}