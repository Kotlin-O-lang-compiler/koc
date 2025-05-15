package koc.sema.exception

import koc.parser.ast.ClassDecl
import koc.parser.formatAsBadToken
import koc.utils.CompileException

class ClassRedefinitionException(cd: ClassDecl, previousDecl: ClassDecl) :
    CompileException(
        "Class '${cd.identifier}' redefinition found.\n${
            formatAsBadToken(
                cd.identifierToken, cd.tokens,
                "Previous definition:\n${formatAsBadToken(previousDecl.identifierToken, previousDecl.tokens)}"
            )
        }"
    ) {
}