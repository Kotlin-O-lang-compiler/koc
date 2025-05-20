package koc.parser.impl

import koc.lex.Token
import koc.lex.TokenKind
import koc.ast.Node
import koc.parser.nextPosition
import koc.core.Diagnostics
import koc.lex.Tokens
import koc.lex.diag
import koc.parser.diag.LackOfToken
import koc.parser.diag.UnexpectedToken

internal class ParserCore(private val diag: Diagnostics) {
    private var idx = -1

    private val tokens = arrayListOf<Token>()

    val currentTokens: List<Token> get() = tokens

    val currentIdx: Int get() = idx

    val current: Token? get() = if (idx in tokens.indices) tokens[idx] else null
    val next: Token? get() = if (nextNotCommentIdx() in tokens.indices) tokens[nextNotCommentIdx()] else null

    val allTokens: Tokens
        get() = Tokens(tokens)

    private var isBad = false

    val scope: ParseScope
        get() = ParseScope(
            curScopeKind,
            curScope.joinToString(separator = "") { "${scopeMap.getOrDefault(it, 'a')}$it" })
    private var curScopeKind: ParseScopeKind = ParseScopeKind.DEFAULT
    private val curScope = arrayListOf<Int>()

    private val scopeMap = mutableMapOf<Int, Char>(0 to 'a')

    fun <T : Node> parse(action: ParserCore.() -> T): T = action(this).apply { fillInfo() }
    fun <T : Node, L : Collection<T>> parse(action: ParserCore.() -> L): L = action(this).apply { fillInfo() }

    private inline fun <T> inScope(scope: ParseScopeKind, crossinline action: ParserCore.() -> T): T {
        val scopeBefore = curScopeKind
        curScope += curScope.lastOrNull()?.let { it + 1 } ?: 0
        scopeMap.putIfAbsent(curScope.last(), 'a')
        curScopeKind = scope

        val res = action()

        scopeMap[curScope.last()] = scopeMap.getOrDefault(curScope.last(), 'a').inc()
        scopeMap -= curScope.last() + 1
        curScope.removeLast()
        curScopeKind = scopeBefore
        return res
    }

    fun <T : Node> withScope(scope: ParseScopeKind, action: ParserCore.() -> T): T = inScope(scope) {
        parse(action).fillInfo()
    }

    fun <T : Node, L : Collection<T>> withScope(scope: ParseScopeKind, action: ParserCore.() -> L): L = inScope(scope) {
        parse(action).fillInfo()
    }

    fun expect(tokenKind: TokenKind, lookahead: Boolean = false): Token {
        if (isBad) return Token.invalid
        val nxt = next()
        if (lookahead) previous()
        if (nxt == null) {
            isBad = true
            diag.diag(LackOfToken(tokenKind), tokens.nextPosition)
            return Token.invalid
        } else if (nxt.kind != tokenKind) {
            isBad = true
            diag.diag(UnexpectedToken(nxt, tokenKind), nxt)
            return Token.invalid
        }

        return nxt
    }

    fun expect(kinds: Collection<TokenKind>, lookahead: Boolean = false): Token {
        require(kinds.isNotEmpty())
        for (kind in kinds) {
            if (next?.kind == kind) return expect(kind, lookahead)
        }
        val nxt = next()
        if (nxt == null) {
            isBad = true
            diag.diag(LackOfToken(kinds), tokens.nextPosition)
        } else {
            isBad = true
            diag.diag(UnexpectedToken(nxt, kinds), nxt)
        }
        return Token.invalid
    }

    fun next(): Token? {
        val nxt = next
        nxt?.let { idx = nextNotCommentIdx() }
        return nxt
    }

    private fun previous() {
        if (idx > 0) {
            idx--
        }
        while (idx > 0 && current?.kind == TokenKind.COMMENT) {
            idx--
        }
    }

    fun revive() {
        isBad = false
    }

    fun skip(until: TokenKind): Token {
        var cur = current
        while (cur != null && cur.kind != until) {
            cur = next()
        }
        return cur?.also { revive() } ?: Token.invalid.also { isBad = true }
    }

    fun feed(tokens: List<Token>) {
        this.tokens.clear()
        idx = -1
        this.tokens += tokens
    }

    private fun nextNotCommentIdx(): Int {
        var increment = 1
        while (tokens.size > idx + increment && tokens[idx + increment].kind == TokenKind.COMMENT) {
            increment++
        }
        return idx + increment
    }

    fun <T: Node> T.fillInfo(): T {
        specifyScope(this@ParserCore.scope)
        specifyTokens(this@ParserCore.tokens)
        return this
    }

    fun <T: Node, L : Collection<T>> L.fillInfo(): L {
        forEach { node ->
            node.specifyScope(this@ParserCore.scope)
            node.specifyTokens(this@ParserCore.tokens)
        }
        return this
    }
}

data class ParseScope(val kind: ParseScopeKind, val value: String)

enum class ParseScopeKind {
    CLASS, CLASS_BODY, VAR, METHOD, BODY, DEFAULT, WHILE_BODY
}