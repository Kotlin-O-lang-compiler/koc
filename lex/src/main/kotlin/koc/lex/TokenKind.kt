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
            else -> this.value
        }

    fun toTokenClass() = when (this) {
        CLASS -> Token.CLASS::class
        EXTENDS -> Token.EXTENDS::class
        IS -> Token.IS::class
        END -> Token.END::class
        VAR -> Token.VAR::class
        METHOD -> Token.METHOD::class
        THIS -> Token.THIS::class
        WHILE -> Token.WHILE::class
        LOOP -> Token.LOOP::class
        IF -> Token.IF::class
        THEN -> Token.THEN::class
        ELSE -> Token.ELSE::class
        RETURN -> Token.RETURN::class

        DOT -> Token.DOT::class
        COMMA -> Token.COMMA::class
        COLON -> Token.COLON::class
        ASSIGN -> Token.ASSIGN::class
        WIDE_ARROW -> Token.WIDE_ARROW::class
        LPAREN -> Token.LPAREN::class
        RPAREN -> Token.RPAREN::class
        LSQUARE -> Token.LSQUARE::class
        RSQUARE -> Token.RSQUARE::class
        COMMENT -> Token.COMMENT::class

        TRUE -> Token.TRUE::class
        FALSE -> Token.FALSE::class

        IDENTIFIER -> Token.IDENTIFIER::class
        INT_LITERAL -> Token.INT_LITERAL::class
        REAL_LITERAL -> Token.REAL_LITERAL::class
        INVALID -> Token.INVALID::class
    }

    companion object {
        val specials = listOf(
            LPAREN, RPAREN, LSQUARE, RSQUARE, ASSIGN, WIDE_ARROW, DOT, COMMA, COLON
        )

        val asValues = entries.map { it.value }

        fun fromValue(value: String): TokenKind {
            require(value.isNotEmpty())
            return entries.find { it.value == value }!!
        }
    }
}
