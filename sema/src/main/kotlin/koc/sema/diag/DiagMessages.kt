package koc.sema.diag

import koc.ast.*
import koc.core.DiagMessage
import koc.lex.asWindow
import koc.lex.formatTokens
import koc.parser.ast.Identifier
import java.util.Locale

class BuiltInClassRedefinition(val classDecl: ClassDecl) : DiagMessage(BuiltInClassRedefinitionKind) {
    override fun toString(): String = "Built-in class ${classDecl.identifier} redefinition found"

    override val code: List<String>
        get() = classDecl.allCode
}

class DeclRedefinition(val decl: Decl, val previousDecl: Decl) : DiagMessage(DeclRedefinitionKind) {
    init {
        require(decl.identifier == previousDecl.identifier)
    }

    override val code: List<String>
        get() = decl.allCode

    override fun toString(): String = "${previousDecl.capitalizedDeclKind()} '${decl.identifier}' redefinition found"

    override val extraMessage: String?
        get() = "Previous definition:\n${
            formatTokens(
                previousDecl.window,
                previousDecl.identifierToken.asWindow(previousDecl.window.allTokens),
                showHighlightedPos = true,
                leadingLines = 0u,
                trailingLines = 0u
            )
        }"

    private fun Decl.capitalizedDeclKind(): String = this.declKindValue.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
    }
}

class UnsupportedUserDefinedGenericClass(val classDecl: ClassDecl) : DiagMessage(UnsupportedUserDefinedGenericClassKind) {
    override fun toString(): String = "User-defined generic class is unsupported"

    override val code: List<String>
        get() = classDecl.allCode
}

class RecursiveInheritance(val relatedClass: ClassDecl, val chain: List<Identifier>) : DiagMessage(RecursiveInheritanceKind) {
    override fun toString(): String = "Class '${relatedClass.identifier}' is recursively inherited: ${chain.joinToString(" - ") { "'$it'" }}"

    override val code: List<String>
        get() = relatedClass.allCode
}

class UndefinedReference(val ref: RefExpr) : DiagMessage(UndefinedReferenceKind) {
    override fun toString(): String = "Undefined reference '${ref.identifier}'"

    override val code: List<String>
        get() = ref.allCode
}

class ThisOutOfContext(val ref: RefExpr) : DiagMessage(ThisOutOfContextKind) {
    override fun toString(): String = "Reference to '${ref.identifier}' is out of class context"

    override val code: List<String>
        get() = ref.allCode
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

    override val code: List<String>
        get() = method.allCode
}

class NoSuitableMethodCandidate(val call: CallExpr) : DiagMessage(NoSuitableCandidateKind) {
    override fun toString(): String = "No suitable method candidate '${call.ref.identifier}' on '${(call.ref.ref!! as MethodDecl).outerDecl.identifier}'"

    override val code: List<String>
        get() = call.allCode
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

    override val code: List<String>
        get() = ctor.allCode
}

class NoSuitableConstructorCandidate(val call: CallExpr, val classDecl: ClassDecl) : DiagMessage(NoSuitableCandidateKind) {
    override fun toString(): String = "No suitable constructor on '${classDecl.identifier}'"

    override val code: List<String>
        get() = call.allCode
}

class MemberAccessOnClass(val memberAccess: MemberAccessExpr) : DiagMessage(MemberAccessOnClassKind) {
    override fun toString(): String = "Member access on class reference is prohibited"

    override val code: List<String>
        get() = memberAccess.allCode
}

class MethodReferenceWithoutCall(val ref: RefExpr) : DiagMessage(MethodReferenceWithoutCallKind) {
    override fun toString(): String = "Method reference ('${ref.identifier}') without call is prohibited"

    override val code: List<String>
        get() = ref.allCode
}

class NonReturningCallInExpr(val expr: Expr) : DiagMessage(NonReturningCallInExprKind) {
    override fun toString(): String = "Non-returning method call cannot be a part of expression"

    override val code: List<String>
        get() = expr.allCode
}

class UnableToInferVariableType(val decl: VarDecl) : DiagMessage(UnableToInferVariableTypeKind) {
    override fun toString(): String = "Unable to infer variable '${decl.identifier} type'"

    override val code: List<String>
        get() = decl.allCode
}
