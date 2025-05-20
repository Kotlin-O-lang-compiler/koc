package koc.lex.diag

import koc.core.DiagKind

abstract class LexDiagKind(/*kind: String,*/ verbosity: Verbosity) : DiagKind(/*kind,*/ verbosity)

object IntegerLiteralValueOutOfBoundsKind : LexDiagKind(Verbosity.ERROR)

object UnexpectedTokenKind : LexDiagKind(Verbosity.ERROR)