package koc.parser.impl

import koc.ast.Node
import koc.core.Diagnostics
import koc.lex.Token
import koc.lex.TokenKind
import koc.lex.Tokens
import koc.lex.diag
import koc.parser.diag.LackOfToken
import koc.parser.diag.UnexpectedToken
import koc.parser.nextPosition

internal class ParserCore(private val diag: Diagnostics) {
    private var idx = -1

    private var tokens: Tokens = Tokens(emptyList(), emptyList())

    val currentTokens: List<Token> get() = tokens.tokens

    val currentIdx: Int get() = idx

    val current: Token? get() = if (idx in tokens.tokens.indices) tokens.tokens[idx] else null
    val next: Token? get() = if (nextNotCommentIdx() in tokens.tokens.indices) tokens.tokens[nextNotCommentIdx()] else null

    val allTokens: Tokens
        get() = tokens

    private var isBad = false

    var scope: ParseScope = ParseScope.topLevel
        private set

    fun <T : Node> parse(action: ParserCore.() -> T): T = action(this).apply { fillInfo() }
    fun <T : Node, L : Collection<T>> parse(action: ParserCore.() -> L): L = action(this).apply { fillInfo() }

    private inline fun <T> inScope(scope: ParseScopeKind, crossinline action: ParserCore.() -> T): T {
        val scopeBefore = this.scope
        this.scope = this.scope.nest(scope)

        val res = action()

        this.scope = scopeBefore

        return res
    }

    fun <T> withScope(scope: ParseScopeKind, action: ParserCore.() -> T): T = inScope(scope) {
        this.action()
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
            diag.diag(LackOfToken(tokenKind, allTokens.code), tokens.tokens.nextPosition)
            return Token.invalid
        } else if (nxt.kind != tokenKind) {
            isBad = true
            diag.diag(UnexpectedToken(nxt, tokenKind, allTokens.code), nxt)
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
            diag.diag(LackOfToken(kinds, allTokens.code), tokens.tokens.nextPosition)
        } else {
            isBad = true
            diag.diag(UnexpectedToken(nxt, kinds, allTokens.code), nxt)
        }
        return Token.invalid
    }

    fun next(): Token? {
        val nxt = next
        nxt?.let { idx = nextNotCommentIdx() }
        return nxt
    }

    fun previous() {
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

    fun skip(until: TokenKind, lookahead: Boolean = true): Token {
        var cur = current
        while (cur != null && cur.kind != until) {
            cur = next()
        }
        cur?.let { if (lookahead) previous() }
        return cur?.also { revive() } ?: Token.invalid.also { isBad = true }
    }

    fun skip(vararg until: TokenKind, lookahead: Boolean = true): Token {
        var cur = current
        while (cur != null && cur.kind !in until) {
            cur = next()
        }
        cur?.let { if (lookahead) previous() }
        return cur?.also { revive() } ?: Token.invalid.also { isBad = true }
    }

    fun feed(tokens: Tokens) {
        this.tokens = tokens
        idx = -1
    }

    private fun nextNotCommentIdx(): Int {
        var increment = 1
        while (tokens.tokens.size > idx + increment && tokens.tokens[idx + increment].kind == TokenKind.COMMENT) {
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

@ConsistentCopyVisibility
data class ParseScope private constructor(
    val kind: ParseScopeKind = ParseScopeKind.DEFAULT,
    val depth: UInt = 0u, val width: UInt = 0u, val parent: ParseScope? = null
) {
    private val children = arrayListOf<ParseScope>()

    fun nest(kind: ParseScopeKind): ParseScope {
        val nextNestedWidth = children.lastOrNull()?.width?.plus(1u) ?: 0u
        val child = ParseScope(kind, depth.inc(), nextNestedWidth, this)
        children += child
        return child
    }

    fun inside(): ParseScope {
        return children.first()
    }

    tailrec fun forEach(withParent: (ParseScope) -> Unit) {
        withParent(this)
        if (parent != null) forEach(withParent)
    }

    fun any(withParent: (ParseScope) -> Boolean): Boolean {
        if (withParent(this)) return true
        return parent?.any(withParent) == true
    }

    fun all(withParent: (ParseScope) -> Boolean): Boolean {
        if (!withParent(this)) return false
        return parent?.all(withParent) != false
    }

    fun firstOrNull(of: (ParseScope) -> Boolean): ParseScope? {
        parent?.firstOrNull(of)?.let { return it }
        return if (of(this)) this else null
    }

    fun lastOrNull(of: (ParseScope) -> Boolean): ParseScope? {
        if (of(this)) return this
        return parent?.firstOrNull(of)
    }

    operator fun contains(inner: ParseScope): Boolean {
        return this.scopeValue().contains(inner.scopeValue())
    }

    private fun subscopeValue() = "${'a' + depth.toInt()}${width}"

    private fun scopeValue(builder: StringBuilder = StringBuilder()): String {
        parent?.also { parent -> parent.scopeValue(builder) }
        builder.append(subscopeValue())
        return builder.toString()
    }

    override fun toString(): String = "Scope($kind: ${scopeValue()})"

    companion object {
        val topLevel = ParseScope()
    }
}

enum class ParseScopeKind {
    CLASS, CLASS_BODY, VAR, METHOD, CONSTRUCTOR, BODY, DEFAULT, WHILE_BODY
}