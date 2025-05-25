package koc.sema.diag

import koc.core.DiagKind


abstract class SemaDiagKind(verbosity: Verbosity) : DiagKind(verbosity)

object BuiltInClassRedefinitionKind : SemaDiagKind(Verbosity.ERROR)
object DeclRedefinitionKind : SemaDiagKind(Verbosity.ERROR)
object RecursiveInheritanceKind : SemaDiagKind(Verbosity.ERROR)
object UnsupportedUserDefinedGenericClassKind : SemaDiagKind(Verbosity.ERROR)
object UndefinedReferenceKind : SemaDiagKind(Verbosity.ERROR)
object ThisOutOfContextKind : SemaDiagKind(Verbosity.ERROR)
object OverloadResolutionFailedKind : SemaDiagKind(Verbosity.ERROR)
object NoSuitableCandidateKind : SemaDiagKind(Verbosity.ERROR)
object MemberAccessOnClassKind : SemaDiagKind(Verbosity.ERROR)
object MethodReferenceWithoutCallKind : SemaDiagKind(Verbosity.ERROR)
object NonReturningCallInExprKind : SemaDiagKind(Verbosity.ERROR)
object UnableToInferVariableTypeKind : SemaDiagKind(Verbosity.ERROR)