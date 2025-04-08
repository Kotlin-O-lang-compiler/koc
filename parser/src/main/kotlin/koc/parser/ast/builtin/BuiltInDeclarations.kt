package koc.parser.ast.builtin

import koc.lex.Token
import koc.lex.TokenKind
import koc.parser.ast.ClassDecl
import koc.utils.Position

private val fakeStdPos = Position(0u, 0u, "std")

//private class RootClassDecl : ClassDecl(
//    Token(TokenKind.CLASS, fakeStdPos),
//    Token("Class", TokenKind.IDENTIFIER, Position, fakeStdPos),
//) {
//}