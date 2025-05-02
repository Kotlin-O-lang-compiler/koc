package koc.lex


enum class TokenKind(val value: String) {
     CLASS("class"),
     EXTENDS("extends"),
     IS("is"),
     END("end"),
     VAR("var"),
     METHOD("method"),
     THIS("this"),
     WHILE("while"),
     LOOP("loop"),
     IF("if"),
     THEN("then"),
     ELSE("else"),
     RETURN("return"),

     DOT("."),
     COMMA(","),
     COLON(":"),
     ASSIGN(":="),
     WIDE_ARROW("=>"),
     LPAREN("("),
     RPAREN(")"),
     LSQUARE("["),
     RSQUARE("]"),
    /**
     * Single-line comment
     */
    COMMENT("//"),

    TRUE("true"),
    FALSE("false"),

    // dynamic value tokens
    IDENTIFIER(""),
    INT_LITERAL(""),
    REAL_LITERAL(""),

    INVALID("");

    val size: UInt get() = value.length.toUInt()

    val diagValue: String
        get() = when (this) {
            IDENTIFIER -> "identifier"
            INT_LITERAL -> "integer literal"
            REAL_LITERAL -> "real literal"
            else -> "'${this.value}'"
        }

    companion object {
        val asValues = entries.map { it.value }

        fun fromValue(value: String): TokenKind {
            require(value.isNotEmpty())
            return entries.find { it.value == value }!!
        }
    }
}
