package koc.parser.impl

import koc.core.Diagnostics
import koc.lex.Token
import koc.lex.TokenKind
import koc.lex.diag
import koc.parser.Parser
import koc.ast.Argument
import koc.ast.Assignment
import koc.parser.ast.Attribute
import koc.ast.Body
import koc.ast.BooleanLiteral
import koc.ast.CallExpr
import koc.ast.ClassBody
import koc.ast.ClassDecl
import koc.ast.ClassMemberDecl
import koc.ast.ConstructorDecl
import koc.ast.Expr
import koc.ast.FieldDecl
import koc.ast.GenericParams
import koc.ast.IfNode
import koc.ast.IntegerLiteral
import koc.ast.InvalidExpr
import koc.ast.MemberAccessExpr
import koc.ast.MethodBody
import koc.ast.MethodDecl
import koc.ast.Node
import koc.ast.Param
import koc.ast.Params
import koc.ast.RealLiteral
import koc.ast.RefExpr
import koc.ast.ReturnNode
import koc.ast.Statement
import koc.ast.TypeParam
import koc.ast.VarDecl
import koc.ast.WhileNode
import koc.parser.diag.LackOfNode
import koc.parser.diag.OtherNodeExpected
import koc.parser.diag.UnexpectedToken
import koc.parser.loadIfEmpty
import koc.parser.next

class ParserImpl(
    val diag: Diagnostics
) : Parser {
    private val core = ParserCore(diag)

    val currentTokenIdx: Int get() = core.currentIdx

    override fun parseNodes(tokens: List<Token>): List<Node> {
        diag.loadIfEmpty(tokens)
        var token = 0
        val nodes = arrayListOf<Node>()

        core.feed(tokens)

        while (token < tokens.size) {
            nodes += parseClassDecl()
            token = currentTokenIdx + 1
        }
        return nodes
    }

    override fun parseClassDecl(tokens: List<Token>): ClassDecl {
        diag.loadIfEmpty(tokens)
        core.feed(tokens)
        return parseClassDecl()
    }

    override fun parseClassBody(tokens: List<Token>): ClassBody {
        diag.loadIfEmpty(tokens)
        core.feed(tokens)
        return parseClassBody()
    }

    override fun parseClassMemberDecl(tokens: List<Token>): ClassMemberDecl {
        diag.loadIfEmpty(tokens)
        core.feed(tokens)
        return parseClassMemberDecl()
    }

    override fun parseVarDecl(tokens: List<Token>): VarDecl {
        diag.loadIfEmpty(tokens)
        core.feed(tokens)
        return parseVarDecl()
    }

    override fun parseMethod(tokens: List<Token>): MethodDecl {
        diag.loadIfEmpty(tokens)
        core.feed(tokens)
        return parseMethod()
    }

    override fun parseConstructor(tokens: List<Token>): ConstructorDecl {
        diag.loadIfEmpty(tokens)
        core.feed(tokens)
        return parseConstructor()
    }

    override fun parseExpr(tokens: List<Token>): Expr {
        diag.loadIfEmpty(tokens)
        core.feed(tokens)
        return parseExpr()
    }

    override fun parseIntegerLiteral(tokens: List<Token>): IntegerLiteral {
        diag.loadIfEmpty(tokens)
        core.feed(tokens)
        return parseIntegerLiteral()
    }

    override fun parseRealLiteral(tokens: List<Token>): RealLiteral {
        diag.loadIfEmpty(tokens)
        core.feed(tokens)
        return parseRealLiteral()
    }

    override fun parseBooleanLiteral(tokens: List<Token>): BooleanLiteral {
        diag.loadIfEmpty(tokens)
        core.feed(tokens)
        return parseBooleanLiteral()
    }

    override fun parseRefExpr(tokens: List<Token>): RefExpr {
        diag.loadIfEmpty(tokens)
        core.feed(tokens)
        return parseRefExpr()
    }

    override fun parseWhileLoop(tokens: List<Token>): WhileNode {
        diag.loadIfEmpty(tokens)
        core.feed(tokens)
        return parseWhileLoop()
    }

    override fun parseIfNode(tokens: List<Token>): IfNode {
        diag.loadIfEmpty(tokens)
        core.feed(tokens)
        return parseIf()
    }

    override fun parseAssignment(tokens: List<Token>): Assignment {
        diag.loadIfEmpty(tokens)
        core.feed(tokens)
        return parseAssignment()
    }

    override fun parseReturnNode(tokens: List<Token>): ReturnNode {
        diag.loadIfEmpty(tokens)
        core.feed(tokens)
        return parseReturn()
    }

    private fun parseClassDecl(): ClassDecl = core.parse<ClassDecl> {
        val classToken = expect(TokenKind.CLASS)
        val ref = parseRefExpr(thisAllowed = false) // parse identifier and generic params

        val extendsToken = if (next?.kind == TokenKind.EXTENDS) expect(TokenKind.EXTENDS) else null
        val (superTypeRef, body) = withScope<Pair<RefExpr?, ClassBody>>(ParseScopeKind.CLASS) {
            extendsToken?.let { parseRefExpr(thisAllowed = false) } to parseClassBody()
        }

        ClassDecl(classToken, ref.identifierToken, ref.generics, extendsToken, superTypeRef, body)
    }

    private fun parseClassBody(): ClassBody = core.parse<ClassBody> {
        val isToken = expect(TokenKind.IS)
        val members = arrayListOf<ClassMemberDecl>()
        core.withScope<Unit>(ParseScopeKind.CLASS_BODY) {
            while (next != null && next?.kind != TokenKind.END) {
                members += parseClassMemberDecl()
            }
        }

        val endToken = expect(TokenKind.END)
        val body = ClassBody(isToken, endToken)
        body += members
        body
    }

    private fun parseClassMemberDecl(): ClassMemberDecl = core.parse<ClassMemberDecl> {
        val first = core.expect(classMemberStartToken, lookahead = true)

        return@parse when (first.kind) {
            TokenKind.VAR -> FieldDecl(parseVarDecl())
            TokenKind.METHOD -> parseMethod()
            TokenKind.THIS -> parseConstructor()
            else -> {
                if (first.kind != TokenKind.INVALID)
                    diag.diag(OtherNodeExpected(listOf(FieldDecl::class.simpleName!!, MethodDecl::class.simpleName!!,
                        ConstructorDecl::class.simpleName!!)), first)
                MethodDecl(Token.invalid, Token.invalid) // invalid decl
            }
        }
    }

    private fun parseVarDecl(): VarDecl = core.parse<VarDecl> {
        val keyword = expect(TokenKind.VAR)
        val id = expect(TokenKind.IDENTIFIER)
        val colon = expect(TokenKind.COLON)
        val expr = core.withScope<Expr>(ParseScopeKind.VAR) { parseExpr() }

        VarDecl(keyword, id, colon, expr)
    }

    private fun parseMethod(): MethodDecl = core.parse<MethodDecl> {
        val keyword = expect(TokenKind.METHOD)
        val id = expect(TokenKind.IDENTIFIER)
        var params: Params? = null
        var colon: Token? = null
        var retType: RefExpr? = null
        var body: MethodBody? = null

        run {
            if (next == null) return@run
            withScope<Unit>(ParseScopeKind.METHOD) {
                val afterId = expect(
                    classMemberStartToken + TokenKind.END + TokenKind.LPAREN + TokenKind.COLON + TokenKind.IS + TokenKind.WIDE_ARROW,
                    lookahead = true
                )
                if (afterId.kind in classMemberStartToken + TokenKind.END) return@withScope

                params = when (afterId.kind) {
                    TokenKind.LPAREN -> parseParams()
                    else -> null
                }

                val afterParams = when {
                    afterId.kind == TokenKind.COLON -> afterId
                    next == null -> return@withScope
                    else -> expect(
                        classMemberStartToken + TokenKind.END + TokenKind.COLON + TokenKind.IS + TokenKind.WIDE_ARROW,
                        lookahead = true
                    )
                }

                if (afterParams.kind in classMemberStartToken + TokenKind.END) return@withScope

                colon = if (afterParams.kind == TokenKind.COLON) expect(TokenKind.COLON) else null
                retType = colon?.let { parseRefExpr() }

                val afterRetType = when {
                    afterParams.kind in listOf(TokenKind.IS, TokenKind.WIDE_ARROW) -> afterParams
                    next == null -> return@withScope
                    else -> expect(
                        classMemberStartToken + TokenKind.END + TokenKind.IS + TokenKind.WIDE_ARROW,
                        lookahead = true
                    )
                }

                if (afterRetType.kind in classMemberStartToken + TokenKind.END) return@withScope

                body = when (afterRetType.kind) {
                    TokenKind.IS -> core.parse<MethodBody> { MethodBody.MBody(parseMethodBody()) }
                    TokenKind.WIDE_ARROW -> core.parse<MethodBody> {
                        val arrow = expect(TokenKind.WIDE_ARROW)
                        MethodBody.MExpr(arrow, withScope<Expr>(ParseScopeKind.BODY) { parseExpr() })
                    }

                    else -> null
                }
            }
        }

        return@parse MethodDecl(keyword, id, params, colon, retType, body)
    }

    private fun parseConstructor(): ConstructorDecl = core.parse<ConstructorDecl> {
        val thisToken = expect(TokenKind.THIS)
        val afterThis = core.expect(listOf(TokenKind.LPAREN, TokenKind.IS), lookahead = true)
        val params = when (afterThis.kind) {
            TokenKind.LPAREN -> parseParams()
            else -> null
        }
        val body = parseBody()
        ConstructorDecl(thisToken, params, body)
    }

    private fun parseParams(): Params = core.parse<Params> {
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

        Params(lparen, rparen).also { it += params }
    }

    private fun parseParam(): Param = core.parse<Param> {
        val first = core.expect(listOf(TokenKind.IDENTIFIER, TokenKind.COMMA))

        val comma = if (first.kind == TokenKind.COMMA) first else null
        val id = if (first.kind == TokenKind.IDENTIFIER) first else core.expect(TokenKind.IDENTIFIER)
        val colon = core.expect(TokenKind.COLON)
        val type = parseRefExpr()
        return@parse Param(id, colon, type, comma)
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
    ): Body = core.parse<Body> {
        val startToken = if (start.isEmpty()) null else expect(start.toList())
        val nodes = core.withScope<Node, List<Node>>(scope) {
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
        }
    }

    private fun parseMethodBody(): Body = parseBody()

    private fun parseArguments(): List<Argument> = core.parse<Argument, List<Argument>> {
        val args = arrayListOf<Argument>()

        if (core.next?.kind != TokenKind.RPAREN) {
            args += Argument(parseExpr())
        }

        while (core.next?.kind != TokenKind.RPAREN) {
            val comma = core.expect(TokenKind.COMMA)
            args += Argument(parseExpr(), comma)
        }

        return@parse args
    }

    private fun parseExpr(): Expr = core.parse<Expr> {
        val nxt = next
            ?: return@parse diag.diag(LackOfNode(listOf(Expr::class.simpleName!!)), core.current?.end.next())
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
                        TokenKind.DOT -> MemberAccessExpr(ref, afterThis, parseExpr())
                        TokenKind.LPAREN -> {
                            val args = parseArguments()
                            val rparen = expect(TokenKind.RPAREN)
                            CallExpr(ref, afterThis, rparen).apply { this += args }
                        }
                        else -> throw IllegalStateException("`(` or `.`")
                    }
                } else ref
            }

            else -> diag.diag(OtherNodeExpected(listOf(Expr::class.simpleName!!)), nxt).let {
                InvalidExpr()
            }
        }
    }

    private fun parseIntegerLiteral(): IntegerLiteral = core.parse<IntegerLiteral> {
        val token = core.expect(TokenKind.INT_LITERAL)
        IntegerLiteral(token)
    }

    private fun parseRealLiteral(): RealLiteral = core.parse<RealLiteral> {
        val token = core.expect(TokenKind.REAL_LITERAL)
        RealLiteral(token)
    }

    private fun parseBooleanLiteral(): BooleanLiteral = core.parse<BooleanLiteral> {
        val token = core.expect(listOf(TokenKind.TRUE, TokenKind.FALSE))
        BooleanLiteral(token)
    }

    private fun parseRefExpr(thisAllowed: Boolean = true): RefExpr = core.parse<RefExpr> {
        val identifier = core.expect(listOfNotNull(TokenKind.IDENTIFIER, if (thisAllowed) TokenKind.THIS else null))

        if (core.next?.kind != TokenKind.LSQUARE) return@parse RefExpr(identifier)

        val lsquare = core.expect(TokenKind.LSQUARE)
        val params = arrayListOf<TypeParam>()
        params += core.parse<TypeParam> { TypeParam(parseRefExpr()) }

        while (core.next?.kind == TokenKind.COMMA) {
            val comma = core.expect(TokenKind.COMMA)
            val typeRef = parseRefExpr()
            val typeParam = core.parse<TypeParam> { TypeParam(typeRef, comma) }
            params += typeParam
        }

        val rsquare = core.expect(TokenKind.RSQUARE)

        val generics = core.parse<GenericParams> { GenericParams(lsquare, rsquare).also { it += params } }
        return@parse RefExpr(identifier, generics).apply {
            if (isThis) {
                enable(Attribute.BROKEN)
                diag.diag(UnexpectedToken(lsquare, listOf()), lsquare)
            }
        }
    }

    private fun parseAssignment(): Assignment = core.parse<Assignment> {
        val lhs = expect(TokenKind.IDENTIFIER)
        val eq = expect(TokenKind.ASSIGN)
        val rhs = parseExpr()
        Assignment(lhs, eq, rhs)
    }

    private fun parseWhileLoop(): WhileNode = core.parse<WhileNode> {
        val keyword = expect(TokenKind.WHILE)
        val cond = parseExpr()
        val loopToken = expect(TokenKind.LOOP)

        val statements = core.withScope<Node, List<Node>>(ParseScopeKind.WHILE_BODY) {
            parseBodyNodes()
        }
        val endToken = expect(TokenKind.END)
        val body = core.parse<Body> { Body(loopToken, endToken) }
        for (stmt in statements) {
            when (stmt) {
                is VarDecl -> body += stmt
                is Statement -> body += stmt
                else -> throw IllegalStateException("Only statement or variable declaration")
            }
        }
        WhileNode(keyword, cond, body)
    }

    private fun parseIf(): IfNode = core.parse<IfNode> {
        val keyword = expect(TokenKind.IF)
        val cond = parseExpr()
        val thenBody = parseBody(start = listOf(TokenKind.THEN), end = listOf(TokenKind.ELSE, TokenKind.END))

        val elseBody = when (thenBody.endToken.kind) {
            TokenKind.ELSE -> parseBody(start = listOf())
            TokenKind.END -> null
            else -> throw IllegalStateException("Only `else` or `end` after if-body")
        }

        IfNode(keyword, cond, thenBody, elseBody)
    }

    private fun parseReturn(): ReturnNode = core.parse<ReturnNode> {
        val ret = expect(TokenKind.RETURN)
        val expr = if (next?.kind in exprStartToken) parseExpr() else null
        ReturnNode(ret, expr)
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
        )
    }


}