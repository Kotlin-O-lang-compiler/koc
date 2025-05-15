package koc.parser.impl

import koc.lex.Token
import koc.lex.TokenKind
import koc.parser.ExpectedNodeException
import koc.parser.LackOfNodeException
import koc.parser.Parser
import koc.parser.UnexpectedTokenException
import koc.parser.ast.Argument
import koc.parser.ast.Assignment
import koc.parser.ast.Attribute
import koc.parser.ast.Body
import koc.parser.ast.BooleanLiteral
import koc.parser.ast.CallExpr
import koc.parser.ast.ClassBody
import koc.parser.ast.ClassDecl
import koc.parser.ast.ClassMemberDecl
import koc.parser.ast.ConstructorDecl
import koc.parser.ast.Expr
import koc.parser.ast.FieldDecl
import koc.parser.ast.GenericParams
import koc.parser.ast.IfNode
import koc.parser.ast.IntegerLiteral
import koc.parser.ast.InvalidExpr
import koc.parser.ast.MemberAccessExpr
import koc.parser.ast.MethodBody
import koc.parser.ast.MethodDecl
import koc.parser.ast.Node
import koc.parser.ast.Param
import koc.parser.ast.Params
import koc.parser.ast.RealLiteral
import koc.parser.ast.RefExpr
import koc.parser.ast.ReturnNode
import koc.parser.ast.Statement
import koc.parser.ast.TypeParam
import koc.parser.ast.VarDecl
import koc.parser.ast.WhileNode
import koc.parser.next
import koc.utils.Diagnostics

class ParserImpl(
    val diag: Diagnostics
) : Parser {
    private val core = ParserCore(diag)

    val currentTokenIdx: Int get() = core.currentIdx

    override fun parseNodes(tokens: List<Token>): List<Node> {
        var token = 0
        val nodes = arrayListOf<Node>()

        while (token < tokens.size) {
            nodes += parseClassDecl(tokens.subList(token, tokens.size))
            token += currentTokenIdx + 1
        }
        return nodes
    }

//    override fun parse(tokens: List<Token>): Node {
//        TODO("Not yet implemented, please use concrete node `parse{node}`")
//    }

    override fun parseClassDecl(tokens: List<Token>): ClassDecl {
        core.feed(tokens)
        return parseClassDecl()
    }

    override fun parseClassBody(tokens: List<Token>): ClassBody {
        core.feed(tokens)
        return parseClassBody()
    }

    override fun parseClassMemberDecl(tokens: List<Token>): ClassMemberDecl {
        core.feed(tokens)
        return parseClassMemberDecl()
    }

    override fun parseVarDecl(tokens: List<Token>): VarDecl {
        core.feed(tokens)
        return parseVarDecl()
    }

    override fun parseMethod(tokens: List<Token>): MethodDecl {
        core.feed(tokens)
        return parseMethod()
    }

    override fun parseConstructor(tokens: List<Token>): ConstructorDecl {
        core.feed(tokens)
        return parseConstructor()
    }

    override fun parseExpr(tokens: List<Token>): Expr {
        core.feed(tokens)
        return parseExpr()
    }

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

    override fun parseRefExpr(tokens: List<Token>): RefExpr {
        core.feed(tokens)
        return parseRefExpr()
    }

    override fun parseWhileLoop(tokens: List<Token>): WhileNode {
        core.feed(tokens)
        return parseWhileLoop()
    }

    override fun parseIfNode(tokens: List<Token>): IfNode {
        core.feed(tokens)
        return parseIf()
    }

    override fun parseAssignment(tokens: List<Token>): Assignment {
        core.feed(tokens)
        return parseAssignment()
    }

    override fun parseReturnNode(tokens: List<Token>): ReturnNode {
        core.feed(tokens)
        return parseReturn()
    }

    private fun parseClassDecl(): ClassDecl {
        return core.withScope(ParseScopeKind.CLASS) {
            val classToken = expect(TokenKind.CLASS)
            val ref = parseRefExpr() // parse identifier and generic params

            val extendsToken = if (next?.kind == TokenKind.EXTENDS) expect(TokenKind.EXTENDS) else null
            val superTypeRef = extendsToken?.let { parseRefExpr() }
            val body = parseClassBody()

            ClassDecl(classToken, ref.identifierToken, ref.generics, extendsToken, superTypeRef, body)
        }
    }

    private fun parseClassBody(): ClassBody {
        return core.withScope(ParseScopeKind.CLASS_BODY) {
            val isToken = expect(TokenKind.IS)
            val members = arrayListOf<ClassMemberDecl>()
            while (next?.kind != TokenKind.END) {
                members += parseClassMemberDecl()
            }
            val endToken = expect(TokenKind.END)
            val body = ClassBody(isToken, endToken)
            body += members
            body
        }
    }

    private fun parseClassMemberDecl(): ClassMemberDecl {
        val first = core.expect(classMemberStartToken, lookahead = true)

        return when (first.kind) {
            TokenKind.VAR -> FieldDecl(parseVarDecl())
            TokenKind.METHOD -> parseMethod()
            TokenKind.THIS -> parseConstructor()
            else -> {
                if (first.kind != TokenKind.INVALID)
                    diag.error(ExpectedNodeException("class member", first, core.currentTokens), first.start)
                MethodDecl(Token.invalid, Token.invalid) // invalid decl
            }
        }.apply { specifyScope(core.scope) }
    }

    private fun parseVarDecl(): VarDecl {
        return core.withScope(ParseScopeKind.VAR) {
            val keyword = expect(TokenKind.VAR)
            val id = expect(TokenKind.IDENTIFIER)
            val colon = expect(TokenKind.COLON)
            val expr = parseExpr()

            VarDecl(keyword, id, colon, expr)
        }
    }

    private fun parseMethod(): MethodDecl = core.withScope(ParseScopeKind.METHOD) {
        val keyword = expect(TokenKind.METHOD)
        val id = expect(TokenKind.IDENTIFIER)

        if (next == null) return@withScope MethodDecl(keyword, id)

        val afterId = expect(
            classMemberStartToken + TokenKind.END + TokenKind.LPAREN + TokenKind.COLON + TokenKind.IS + TokenKind.WIDE_ARROW,
            lookahead = true
        )
        if (afterId.kind in classMemberStartToken + TokenKind.END) return@withScope MethodDecl(keyword, id)

        val params = when (afterId.kind) {
            TokenKind.LPAREN -> parseParams()
            else -> null
        }

        val afterParams = when {
            afterId.kind == TokenKind.COLON -> afterId
            next == null -> return@withScope MethodDecl(keyword, id, params)
            else -> expect(
                classMemberStartToken + TokenKind.END + TokenKind.COLON + TokenKind.IS + TokenKind.WIDE_ARROW,
                lookahead = true
            )
        }

        if (afterParams.kind in classMemberStartToken + TokenKind.END) return@withScope MethodDecl(
            keyword, id, params
        )

        val colon = if (afterParams.kind == TokenKind.COLON) expect(TokenKind.COLON) else null
        val retType = colon?.let { parseRefExpr() }

        val afterRetType = when {
            afterParams.kind in listOf(TokenKind.IS, TokenKind.WIDE_ARROW) -> afterParams
            next == null -> return@withScope MethodDecl(keyword, id, params, colon, retType)
            else -> expect(classMemberStartToken + TokenKind.END + TokenKind.IS + TokenKind.WIDE_ARROW, lookahead = true)
        }

        if (afterRetType.kind in classMemberStartToken + TokenKind.END) return@withScope MethodDecl(
            keyword, id, params, colon, retType
        )

        val body = when (afterRetType.kind) {
            TokenKind.IS -> MethodBody.MBody(parseMethodBody())
            TokenKind.WIDE_ARROW -> {
                val arrow = expect(TokenKind.WIDE_ARROW)
                MethodBody.MExpr(arrow, parseExpr())
            }
            else -> null
        }

        MethodDecl(keyword, id, params, colon, retType, body)
    }

    private fun parseConstructor(): ConstructorDecl = with(core) {
        val thisToken = expect(TokenKind.THIS)
        val afterThis = core.expect(listOf(TokenKind.LPAREN, TokenKind.IS), lookahead = true)
        val params = when (afterThis.kind) {
            TokenKind.LPAREN -> parseParams()
            else -> null
        }
        val body = parseBody()
        ConstructorDecl(thisToken, params, body)
    }

    private fun parseParams(): Params {
        val lparen = core.expect(TokenKind.LPAREN)
        val params = arrayListOf<Param>()

        if (core.next?.kind != TokenKind.RPAREN) {
            params += parseParam()
        }

        while (core.next?.kind != TokenKind.RPAREN) {
            val comma = core.expect(TokenKind.COMMA)
            params += parseParam()
        }

        val rparen = core.expect(TokenKind.RPAREN)

        return Params(lparen, rparen).also { it += params }.apply { specifyScope(core.scope) }
    }

    private fun parseParam(): Param {
        val first = core.expect(listOf(TokenKind.IDENTIFIER, TokenKind.COMMA))

        val comma = if (first.kind == TokenKind.COMMA) first else null
        val id = if (first.kind == TokenKind.IDENTIFIER) first else core.expect(TokenKind.IDENTIFIER)
        val colon = core.expect(TokenKind.COLON)
        val type = parseRefExpr()
        return Param(id, colon, type, comma).apply { specifyScope(core.scope) }
    }

    private fun parseBodyNodes(vararg end: TokenKind = arrayOf(TokenKind.END)): List<Node> = with(core) {
        val statements = arrayListOf<Node>()
        while (next?.kind !in end) {
            val lookahead = expect(statementStartToken + TokenKind.VAR, lookahead = true)
            statements += when (lookahead.kind) {
                TokenKind.VAR -> parseVarDecl()
                TokenKind.IDENTIFIER -> parseAssignment()
                TokenKind.WHILE -> parseWhileLoop()
                TokenKind.IF -> parseIf()
                TokenKind.RETURN -> parseReturn()
                else -> throw IllegalStateException("unexpected $lookahead in body")
            }
        }
        statements
    }

    private fun parseBody(
        scope: ParseScopeKind = ParseScopeKind.BODY,
        start: Collection<TokenKind> = listOf(TokenKind.IS),
        end: Collection<TokenKind> = listOf(TokenKind.END)
    ): Body = with(core) {
        val startToken = if (start.isEmpty()) null else expect(start.toList())
        val nodes = core.withScope(scope) {
            parseBodyNodes(*end.toTypedArray())
        }
        val endToken = expect(end.toList())
        Body(startToken, endToken).apply {
            for (node in nodes) {
                when (node) {
                    is Statement -> this += node
                    is VarDecl -> this += node
                    else -> throw IllegalStateException("Only statement or variable declaration")
                }
            }
            specifyScope(core.scope)
        }
    }

    private fun parseMethodBody(): Body = parseBody()

    private fun parseArguments(): List<Argument> {
        val args = arrayListOf<Argument>()

        if (core.next?.kind != TokenKind.RPAREN) {
            args += Argument(parseExpr()).apply { specifyScope(core.scope) }
        }

        while (core.next?.kind != TokenKind.RPAREN) {
            val comma = core.expect(TokenKind.COMMA)
            args += Argument(parseExpr(), comma).apply { specifyScope(core.scope) }
        }

        return args
    }

    private fun parseExpr(): Expr = with(core) {
        val nxt = next
            ?: return@with diag.error(LackOfNodeException("expression", currentTokens), core.current?.end.next())
                .let { InvalidExpr() }
        when (nxt.kind) {
            TokenKind.INT_LITERAL -> parseIntegerLiteral()
            TokenKind.REAL_LITERAL -> parseRealLiteral()
            TokenKind.TRUE -> parseBooleanLiteral()
            TokenKind.FALSE -> parseBooleanLiteral()
            TokenKind.THIS, TokenKind.IDENTIFIER -> {
                val ref = parseRefExpr()
                if (next?.kind in listOf(TokenKind.DOT, TokenKind.LPAREN)) {
                    val afterThis = expect(listOf(TokenKind.DOT, TokenKind.LPAREN))
                    when (afterThis.kind) {
                        TokenKind.DOT -> MemberAccessExpr(ref, afterThis, parseExpr()).apply { specifyScope(core.scope) }
                        TokenKind.LPAREN -> {
                            val args = parseArguments()
                            val rparen = expect(TokenKind.RPAREN)
                            CallExpr(ref, afterThis, rparen).apply { this += args }.apply { specifyScope(core.scope) }
                        }
                        else -> throw IllegalStateException("`(` or `.`")
                    }
                } else ref
            }

            else -> diag.error(ExpectedNodeException("expression", nxt, currentTokens), nxt.start).let {
                InvalidExpr().apply { specifyScope(core.scope) }
            }
        }
    }

    private fun parseIntegerLiteral(): IntegerLiteral {
        val token = core.expect(TokenKind.INT_LITERAL)
        return IntegerLiteral(token).apply { specifyScope(core.scope) }
    }

    private fun parseRealLiteral(): RealLiteral {
        val token = core.expect(TokenKind.REAL_LITERAL)
        return RealLiteral(token).apply { specifyScope(core.scope) }
    }

    private fun parseBooleanLiteral(): BooleanLiteral {
        val token = core.expect(listOf(TokenKind.TRUE, TokenKind.FALSE))
        return BooleanLiteral(token).apply { specifyScope(core.scope) }
    }

    private fun parseRefExpr(): RefExpr {
        val identifier = core.expect(listOf(TokenKind.IDENTIFIER, TokenKind.THIS))

        if (core.next?.kind != TokenKind.LSQUARE) return RefExpr(identifier).apply { specifyScope(core.scope) }

        val lsquare = core.expect(TokenKind.LSQUARE)
        val params = arrayListOf<TypeParam>()
        params += TypeParam(parseRefExpr()).apply { specifyScope(core.scope) }

        while (core.next?.kind == TokenKind.COMMA) {
            val comma = core.expect(TokenKind.COMMA)
            val typeRef = parseRefExpr()
            val typeParam = TypeParam(typeRef, comma).apply { specifyScope(core.scope) }
            params += typeParam
        }

        val rsquare = core.expect(TokenKind.RSQUARE)

        val generics = GenericParams(lsquare, rsquare).also { it += params }.apply { specifyScope(core.scope) }
        return RefExpr(identifier, generics).apply {
            if (isThis) {
                enable(Attribute.BROKEN)
                diag.error(UnexpectedTokenException(lsquare, listOf(), tokens), lsquare.start)
            }
            specifyScope(core.scope)
        }
    }

    private fun parseAssignment(): Assignment = with(core) {
        val lhs = expect(TokenKind.IDENTIFIER)
        val eq = expect(TokenKind.ASSIGN)
        val rhs = parseExpr()
        Assignment(lhs, eq, rhs).apply { specifyScope(core.scope) }
    }

    private fun parseWhileLoop(): WhileNode = with(core) {
        val keyword = expect(TokenKind.WHILE)
        val cond = parseExpr()
        val loopToken = expect(TokenKind.LOOP)

        val statements = core.withScope(ParseScopeKind.WHILE_BODY) {
            parseBodyNodes()
        }
        val endToken = expect(TokenKind.END)
        val body = Body(loopToken, endToken).apply { specifyScope(core.scope) }
        for (stmt in statements) {
            when (stmt) {
                is VarDecl -> body += stmt
                is Statement -> body += stmt
                else -> throw IllegalStateException("Only statement or variable declaration")
            }
        }
        WhileNode(keyword, cond, body).apply { specifyScope(core.scope) }
    }

    private fun parseIf(): IfNode = with(core) {
        val keyword = expect(TokenKind.IF)
        val cond = parseExpr()
        val thenBody = parseBody(start = listOf(TokenKind.THEN), end = listOf(TokenKind.ELSE, TokenKind.END))

        val elseBody = when (thenBody.endToken.kind) {
            TokenKind.ELSE -> parseBody(start = listOf())
            TokenKind.END -> null
            else -> throw IllegalStateException("Only `else` or `end` after if-body")
        }

        IfNode(keyword, cond, thenBody, elseBody).apply { specifyScope(core.scope) }
    }

    private fun parseReturn(): ReturnNode = with(core) {
        val ret = expect(TokenKind.RETURN)
        val expr = if (next?.kind in exprStartToken) parseExpr() else null
        ReturnNode(ret, expr).apply { specifyScope(core.scope) }
    }

    companion object {
        val classMemberStartToken = listOf(TokenKind.VAR, TokenKind.METHOD, TokenKind.THIS)
        val statementStartToken = listOf(TokenKind.IDENTIFIER, TokenKind.WHILE, TokenKind.IF, TokenKind.RETURN)
        val exprStartToken = listOf(
            TokenKind.IDENTIFIER,
            TokenKind.INT_LITERAL,
            TokenKind.REAL_LITERAL,
            TokenKind.TRUE,
            TokenKind.FALSE,
            TokenKind.THIS
        ) // TODO: extra tokens
    }


}