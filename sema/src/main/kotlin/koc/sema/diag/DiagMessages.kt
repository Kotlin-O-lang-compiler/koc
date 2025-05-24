package koc.sema.diag

import koc.core.DiagMessage
import koc.lex.asWindow
import koc.lex.formatTokens
import koc.ast.ClassDecl
import koc.ast.ConstructorDecl
import koc.ast.Decl
import koc.ast.MethodDecl
import koc.ast.RefExpr
import koc.parser.ast.Identifier
import java.util.Locale

class BuiltInClassRedefinition(val classDecl: ClassDecl) : DiagMessage(BuiltInClassRedefinitionKind) {
    override fun toString(): String = "Built-in class ${classDecl.identifier} redefinition found"
}

class DeclRedefinition(val decl: Decl, val previousDecl: Decl) : DiagMessage(DeclRedefinitionKind) {
    init {
        require(decl.identifier == previousDecl.identifier)
    }

    override fun toString(): String = "${previousDecl.capitalizedDeclKind()} '${decl.identifier}' redefinition found"

    override val extraMessage: String?
        get() = "Previous definition:\n${
            formatTokens(
                previousDecl.window,
                previousDecl.identifierToken.asWindow(previousDecl.window.allTokens.tokens),
                showHighlightedPos = true,
                leadingLines = 0u,
                trailingLines = 0u
            )
        }"

    private fun Decl.capitalizedDeclKind(): String = this.declKindValue.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
    }
}

class UnsupportedUserDefinedGenericClass() : DiagMessage(UnsupportedUserDefinedGenericClassKind) {
    override fun toString(): String = "User-defined generic class is unsupported"
}

class RecursiveInheritance(val relatedClass: ClassDecl, val chain: List<Identifier>) : DiagMessage(RecursiveInheritanceKind) {
    override fun toString(): String = "Class '${relatedClass.identifier}' is recursively inherited: ${chain.joinToString(" - ") { "'$it'" }}"
}

class UndefinedReference(val ref: RefExpr) : DiagMessage(UndefinedReferenceKind) {
    override fun toString(): String = "Undefined reference '${ref.identifier}'"
}

class ThisOutOfContext(val ref: RefExpr) : DiagMessage(ThisOutOfContextKind) {
    override fun toString(): String = "Reference to '${ref.identifier}' is out of class context"
}

class MethodOverloadFailed(val method: MethodDecl, val candidates: List<MethodDecl>) : DiagMessage(OverloadResolutionFailedKind) {
    override fun toString(): String = "Overload resolution failed for method '${method.identifier}' of class '${method.outerDecl.identifier}'"

    override val extraMessage: String?
        get() = "Candidates are:\n${
            candidates.joinToString("\n") { candidate -> formatTokens(
                candidate.signatureWindow,
                candidate.signatureWindow,
                showHighlightedPos = true
            ) }
        }"
}

class ConstructorOverloadFailed(val ctor: ConstructorDecl, val candidates: List<ConstructorDecl>) : DiagMessage(OverloadResolutionFailedKind) {
    override fun toString(): String = "Overload resolution failed for constructor of class '${ctor.outerDecl.identifier}'"

    override val extraMessage: String?
        get() = "Candidates are:\n${
            candidates.joinToString("\n") { candidate -> formatTokens(
                candidate.signatureWindow,
                candidate.signatureWindow,
                showHighlightedPos = true
            ) }
        }"
}

