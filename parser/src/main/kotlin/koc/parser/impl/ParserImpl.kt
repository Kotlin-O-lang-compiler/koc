package koc.parser.impl

import koc.lex.Token
import koc.lex.TokenKind
import koc.parser.ExpectedNodeException
import koc.parser.LackOfNodeException
import koc.parser.LackOfTokenException
import koc.parser.Parser
import koc.parser.UnexpectedTokenException
import koc.parser.ast.BooleanLiteral
import koc.parser.ast.ClassDecl
import koc.parser.ast.ClassMemberDecl
import koc.parser.ast.Expr
import koc.parser.ast.GenericParams
import koc.parser.ast.IntegerLiteral
import koc.parser.ast.InvalidExpr
import koc.parser.ast.Node
import koc.parser.ast.RealLiteral
import koc.parser.ast.RefExpr
import koc.parser.ast.ThisExpr
import koc.parser.ast.TypeParam
import koc.parser.ast.VarDecl
import koc.parser.next
import koc.utils.Diagnostics

class ParserImpl(
    val diag: Diagnostics
) : Parser {
    private val core = ParserCore(diag)

    override fun parse(tokens: List<Token>): Node {
        TODO("Not yet implemented")
    }

    override fun parseClassDecl(tokens: List<Token>): ClassDecl {
        TODO("Not yet implemented")
    }

    override fun parseClassMemberDecl(tokens: List<Token>): ClassMemberDecl {
        TODO("Not yet implemented")
    }

    override fun parseVarDecl(tokens: List<Token>): VarDecl {
        core.feed(tokens)
        return parseVarDecl()
    }


    override fun parseExpr(tokens: List<Token>): Expr {
        core.feed(tokens)
        return parseExpr()
    }

//    override fun parsePrimaryExpr(tokens: List<Token>): Expr {
//        core.feed(tokens)
//        return parsePrimaryExpr()
//    }

    override fun parseIntegerLiteral(tokens: List<Token>): IntegerLiteral {
        core.feed(tokens)
        return parseIntegerLiteral()
    }

    override fun parseRealLiteral(tokens: List<Token>): RealLiteral {
        core.feed(tokens)
        return parseRealLiteral()
    }

    override fun parseBooleanLiteral(tokens: List<Token>): BooleanLiteral {
        core.feed(tokens)
        return parseBooleanLiteral()
    }

    override fun parseThisExpr(tokens: List<Token>): ThisExpr {
        core.feed(tokens)
        return parseThisExpr()
    }


    private fun parseVarDecl(): VarDecl {
        return core.withScope(ParserCore.ParseScopeKind.VAR) {
            val keyword = expect(TokenKind.VAR)
            val id = expect(TokenKind.IDENTIFIER)
            val colon = expect(TokenKind.COLON)
            val expr = parseExpr()

            VarDecl(keyword, id, colon, expr)
        }
    }

    private fun parseExpr(): Expr {
        return core.withScope(ParserCore.ParseScopeKind.EXPR) {
            val nxt = next
                ?: return@withScope diag.error(LackOfNodeException("expression", currentTokens), core.current?.end.next())
                    .let { InvalidExpr() }
            when (nxt.kind) {
                TokenKind.INT_LITERAL -> parseIntegerLiteral()
                TokenKind.REAL_LITERAL -> parseRealLiteral()
                TokenKind.TRUE -> parseBooleanLiteral()
                TokenKind.FALSE -> parseBooleanLiteral()
                TokenKind.THIS -> parseThisExpr()

                else -> diag.error(ExpectedNodeException("expression", nxt, currentTokens), nxt.start).let { InvalidExpr() }
            }
        }
    }

    private fun parseIntegerLiteral(): IntegerLiteral {
        val token = core.expect(TokenKind.INT_LITERAL)
        return IntegerLiteral(token)
    }

    private fun parseRealLiteral(): RealLiteral {
        val token = core.expect(TokenKind.REAL_LITERAL)
        return RealLiteral(token)
    }

    private fun parseBooleanLiteral(): BooleanLiteral {
        val token = core.expect(listOf(TokenKind.TRUE, TokenKind.FALSE))
        return BooleanLiteral(token)
    }

    private fun parseThisExpr(): ThisExpr {
        val token = core.expect(TokenKind.THIS)
        return ThisExpr(token)
    }

    private fun parseRefExpr(): RefExpr {
        val identifier = core.expect(TokenKind.IDENTIFIER)

        if (core.next?.kind != TokenKind.LSQUARE) return RefExpr(identifier)

        val lsquare = core.expect(TokenKind.LSQUARE)
        val params = arrayListOf<TypeParam>()
        params += TypeParam(parseRefExpr())

        while (core.next?.kind == TokenKind.COMMA) {
            val comma = core.expect(TokenKind.COMMA)
            val typeRef = parseRefExpr()
            val typeParam = TypeParam(typeRef, comma)
            params += typeParam
        }

        val rsquare = core.expect(TokenKind.RSQUARE)

        val generics = GenericParams(lsquare, rsquare).also { it += params }
        return RefExpr(identifier, generics)
    }


}