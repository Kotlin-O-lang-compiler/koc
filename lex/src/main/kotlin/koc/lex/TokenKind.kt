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
        CLASS -> Token.Keyword::class
        EXTENDS -> Token.Keyword::class
        IS -> Token.Keyword::class
        END -> Token.Keyword::class
        VAR -> Token.Keyword::class
        METHOD -> Token.Keyword::class
        THIS -> Token.Keyword::class
        WHILE -> Token.Keyword::class
        LOOP -> Token.Keyword::class
        IF -> Token.Keyword::class
        THEN -> Token.Keyword::class
        ELSE -> Token.Keyword::class
        RETURN -> Token.Keyword::class

        DOT -> Token.Special::class
        COMMA -> Token.Special::class
        COLON -> Token.Special::class
        ASSIGN -> Token.Special::class
        WIDE_ARROW -> Token.Special::class
        LPAREN -> Token.Special::class
        RPAREN -> Token.Special::class
        LSQUARE -> Token.Special::class
        RSQUARE -> Token.Special::class
        COMMENT -> Token.Special::class

        TRUE -> Token.Keyword::class
        FALSE -> Token.Keyword::class

        IDENTIFIER -> Token.Identifier::class
        INT_LITERAL -> Token.IntLiteral::class
        REAL_LITERAL -> Token.RealLiteral::class
        INVALID -> Token.Invalid::class
    }

    companion object {
        val asValues = entries.map { it.value }

        fun fromValue(value: String): TokenKind {
            require(value.isNotEmpty())
            return entries.find { it.value == value }!!
        }
    }
}
