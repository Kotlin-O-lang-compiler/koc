package koc.sema

import koc.lex.Token
import koc.parser.ast.ClassType
import koc.parser.formatAsBadToken
import koc.utils.CompileException

class TypeMismatchException(val expected: ClassType, val actual: ClassType, val tokens: List<Token>, token: Token) :
    CompileException(
        "Type mismatch\n${formatAsBadToken(token, tokens, "Expected ${expected.classDecl.identifier}")}"
    ) {
}