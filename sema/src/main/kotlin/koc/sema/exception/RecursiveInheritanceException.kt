package koc.sema.exception

import koc.parser.ast.ClassDecl
import koc.utils.CompileException

class RecursiveInheritanceException(cd: ClassDecl) : CompileException("Recursive inheritance found on class ${cd.identifier}") {
}