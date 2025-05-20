package koc.parser.diag

import koc.core.DiagKind

abstract class ParseDiagKind(verbosity: Verbosity) : DiagKind(verbosity)

object LackOfTokenKind : ParseDiagKind(Verbosity.ERROR)
object LackOfNodeKind : ParseDiagKind(Verbosity.ERROR)
object UnexpectedTokenKind : ParseDiagKind(Verbosity.ERROR)
object OtherNodeExpectedKind : ParseDiagKind(Verbosity.ERROR)