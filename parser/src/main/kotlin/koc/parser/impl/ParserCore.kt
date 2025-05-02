package koc.parser.impl

import koc.lex.Token
import koc.lex.TokenKind
import koc.parser.LackOfTokenException
import koc.parser.UnexpectedTokenException
import koc.parser.nextPosition
import koc.utils.Diagnostics

internal class ParserCore(private val diag: Diagnostics) {
    private var idx = -1

    private val tokens = arrayListOf<Token>()

    val currentTokens: List<Token> get() = tokens

    val current: Token? get() = if (idx in tokens.indices) tokens[idx] else null
    val next: Token? get() = if (idx + 1 in tokens.indices) tokens[idx + 1] else null

    private var isBad = false

    val scope: ParseScope
        get() = ParseScope(
            curScopeKind,
            curScope.joinToString(separator = "") { "${scopeMap.getOrDefault(it, 'a')}$it" })
    private var curScopeKind: ParseScopeKind = ParseScopeKind.DEFAULT
    private val curScope = arrayListOf<Int>()

    private val scopeMap = mutableMapOf<Int, Char>(0 to 'a')

    fun <T> withScope(scope: ParseScopeKind, action: ParserCore.() -> T): T {
        val scopeBefore = curScopeKind
        curScope += curScope.lastOrNull()?.let { it + 1 } ?: 0
        scopeMap.putIfAbsent(curScope.last, 'a')
        curScopeKind = scope

        val res = action(this)

        scopeMap[curScope.last] = scopeMap.getOrDefault(curScope.last, 'a').inc()
        scopeMap -= curScope.last + 1
        curScope.removeLast()
        curScopeKind = scopeBefore
        return res
    }

    fun expect(tokenKind: TokenKind, lookahead: Boolean = false): Token {
        if (isBad) return Token.invalid
        val nxt = next()
        if (lookahead) previous()
        if (nxt == null) {
            isBad = true
            diag.error(LackOfTokenException(tokenKind, tokens), tokens.nextPosition)
            return Token.invalid
        } else if (nxt.kind != tokenKind) {
            isBad = true
            diag.error(UnexpectedTokenException(nxt, tokenKind, tokens), nxt.start)
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
            diag.error(LackOfTokenException(kinds, tokens), tokens.nextPosition)
        } else {
            isBad = true
            diag.error(UnexpectedTokenException(nxt, kinds, tokens), nxt.start)
        }
        return Token.invalid
    }

    fun next(): Token? {
        val nxt = next
        nxt?.let { idx++ }
        return nxt
    }

    private fun previous() {
        if (idx > 0) {
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


    data class ParseScope(val kind: ParseScopeKind, val value: String)

    enum class ParseScopeKind {
        CLASS, CLASS_BODY, VAR, EXPR, METHOD, BODY, DEFAULT, WHILE_BODY
    }
}