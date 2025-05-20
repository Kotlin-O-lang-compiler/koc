package koc.sema.diag

import koc.core.DiagMessage
import koc.lex.asWindow
import koc.lex.formatTokens
import koc.ast.ClassDecl

class BuiltInClassRedefinition(val classDecl: ClassDecl) : DiagMessage(BuiltInClassRedefinitionKind) {
    override fun toString(): String = "Built-in class ${classDecl.identifier} redefinition found"
}

class ClassRedefinition(val classDecl: ClassDecl, val previousDecl: ClassDecl) : DiagMessage(ClassRedefinitionKind) {
    override fun toString(): String = "Class '${classDecl.identifier}' redefinition found"

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
}

class UnsupportedUserDefinedGenericClass() : DiagMessage(UnsupportedUserDefinedGenericClassKind) {
    override fun toString(): String = "User-defined generic class is unsupported"
}
