package koc.sema.exception

import koc.parser.ast.ClassDecl
import koc.parser.formatAsBadToken
import koc.utils.CompileException

class BuiltInClassRedefinitionException(cd: ClassDecl) :
    CompileException(
        "Built-in class ${cd.identifier} redefinition found.\n${
            formatAsBadToken(cd.identifierToken, cd.tokens)
        }"
    ) {
}