package koc.sema.diag

import koc.core.DiagKind


abstract class SemaDiagKind(verbosity: Verbosity) : DiagKind(verbosity)

object BuiltInClassRedefinitionKind : SemaDiagKind(Verbosity.ERROR)
object DeclRedefinitionKind : SemaDiagKind(Verbosity.ERROR)
object RecursiveInheritanceKind : SemaDiagKind(Verbosity.ERROR)
object UnsupportedUserDefinedGenericClassKind : SemaDiagKind(Verbosity.ERROR)
object UndefinedReferenceKind : SemaDiagKind(Verbosity.ERROR)
object ThisOutOfContextKind : SemaDiagKind(Verbosity.ERROR)