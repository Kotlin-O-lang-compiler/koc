package koc.sema

import koc.lex.Lexer
import koc.lex.Token
import koc.lex.TokenKind
import koc.parser.Parser
import koc.parser.ast.Attribute
import koc.parser.ast.ClassBody
import koc.parser.ast.ClassDecl
import koc.parser.ast.ClassType
import koc.sema.TypeManager.Companion.ANY_VALUE_ID
import koc.sema.TypeManager.Companion.BOOLEAN_ID
import koc.sema.TypeManager.Companion.CLASS_ID
import koc.sema.TypeManager.Companion.INTEGER_ID
import koc.sema.TypeManager.Companion.REAL_ID
import koc.utils.InternalError
import koc.utils.Position

class TypeManager(lexer: Lexer, parser: Parser) {
    val classType: ClassType
        get() = ClassType(classDecl, null)

    val anyValueType: ClassType
        get() = ClassType(anyValueDecl, classType)

    val intType: ClassType
        get() = ClassType(intDecl, anyValueType)

    val boolType: ClassType
        get() = ClassType(boolDecl, anyValueType)

    val realType: ClassType
        get() = ClassType(realDecl, anyValueType)

    val invalidType: ClassType
        get() = ClassType(invalidDecl, null)

    private val userDefinitions = hashMapOf<String, ClassDecl>()

    fun getUserDefinition(name: String): ClassDecl = userDefinitions[name]!!

    fun hasUserDefinition(name: String): Boolean = name in userDefinitions

    fun learn(userDefinition: ClassDecl) {
        userDefinitions[userDefinition.identifier.value] = userDefinition
    }

    private val types by lazy {
        mutableMapOf<String, ClassType>(
            classType.identifier.value to classType,
            anyValueType.identifier.value to anyValueType,
            intType.identifier.value to intType,
            boolType.identifier.value to boolType,
            realType.identifier.value to realType,
            invalidType.identifier.value to invalidType
        )
    }

    fun getType(name: String): ClassType = types[name]!!

    fun hasType(name: String): Boolean = name in types

    internal fun learn(type: ClassType) {
        if (type.identifier.value in types) throw InternalError("Possible type redefinition ${type.identifier}")
        types[type.identifier.value] = type
    }

    private val classDecl = parser.parseClassDecl(
        lexer.apply { open(CLASS_SOURCE_CODE, STD_FILE_NAME) }.use {
            it.lex()
        }
    ).apply { enable(Attribute.BUILTIN) }

    private val anyValueDecl = parser.parseClassDecl(
        lexer.apply { open(ANY_VALUE_SOURCE_CODE, STD_FILE_NAME) }.use {
            it.lex()
        }
    ).apply { enable(Attribute.BUILTIN) }

    private val intDecl = parser.parseClassDecl(
        lexer.apply { open(INTEGER_SOURCE_CODE, STD_FILE_NAME) }.use {
            it.lex()
        }
    ).apply { enable(Attribute.BUILTIN) }

    private val boolDecl = parser.parseClassDecl(
        lexer.apply { open(BOOLEAN_SOURCE_CODE, STD_FILE_NAME) }.use {
            it.lex()
        }
    ).apply { enable(Attribute.BUILTIN) }

    private val realDecl = parser.parseClassDecl(
        lexer.apply { open(REAL_SOURCE_CODE, STD_FILE_NAME) }.use {
            it.lex()
        }
    ).apply { enable(Attribute.BUILTIN) }

    private val invalidDecl = ClassDecl(
        Token.invalid,
        Token("\$invalid", TokenKind.IDENTIFIER, Position.fake),
        body = ClassBody(Token.invalid, Token.invalid)
    )

    companion object {
        internal val fakeStdPos = Position(0u, 0u, "std")
        const val CLASS_ID = "Class"
        const val ANY_VALUE_ID = "AnyValue"
        const val INTEGER_ID = "Integer"
        const val REAL_ID = "Real"
        const val BOOLEAN_ID = "Boolean"
        val builtinTypes = arrayOf(CLASS_ID, ANY_VALUE_ID, INTEGER_ID, REAL_ID, BOOLEAN_ID)

        private const val STD_FILE_NAME = "std.ol"

        private fun ClassBody() = ClassBody(Token(TokenKind.IS, fakeStdPos), Token(TokenKind.EXTENDS, fakeStdPos))

    }
}

private const val CLASS_SOURCE_CODE = """
class $CLASS_ID is end
"""

private const val ANY_VALUE_SOURCE_CODE = """
class $ANY_VALUE_ID extends $CLASS_ID is end
"""

private const val BOOLEAN_SOURCE_CODE = """
class $BOOLEAN_ID extends $ANY_VALUE_ID is
    this(other: $BOOLEAN_ID) is end
    method toInteger() : $INTEGER_ID
    method Or(p: $BOOLEAN_ID) : $BOOLEAN_ID
    method And(p: $BOOLEAN_ID) : $BOOLEAN_ID
    method Xor(p: $BOOLEAN_ID) : $BOOLEAN_ID
    method Not : $BOOLEAN_ID
end
"""

private const val INTEGER_SOURCE_CODE = """
class $INTEGER_ID extends $ANY_VALUE_ID is
    this(p: $INTEGER_ID) is end
    this(p: $REAL_ID) is end
    
    var Min : $INTEGER_ID //  TODO: Replace with integer literal
    var Max : $INTEGER_ID
    
    method toReal : $REAL_ID
    method toBoolean : $BOOLEAN_ID
    
    method UnaryMinus : $INTEGER_ID
    
    method Plus(p: $INTEGER_ID) : $INTEGER_ID
    method Plus(p: $REAL_ID) : $REAL_ID
    method Minus(p: $INTEGER_ID) : $INTEGER_ID
    method Minus(p: $REAL_ID) : $REAL_ID
    method Mult(p: $INTEGER_ID) : $INTEGER_ID
    method Mult(p: $REAL_ID) : $REAL_ID
    method Div(p: $INTEGER_ID) : $INTEGER_ID
    method Div(p: $REAL_ID) : $REAL_ID
    method Rem(p: $INTEGER_ID) : $INTEGER_ID
    
    method Less(p: $INTEGER_ID) : $BOOLEAN_ID
    method Less(p: $REAL_ID) : $BOOLEAN_ID
    method LessEqual(p: $INTEGER_ID) : $BOOLEAN_ID
    method LessEqual(p: $REAL_ID) : $BOOLEAN_ID
    method Greater(p: $INTEGER_ID) : $BOOLEAN_ID
    method Greater(p: $REAL_ID) : $BOOLEAN_ID
    method GreaterEqual(p: $INTEGER_ID) : $BOOLEAN_ID
    method GreaterEqual(p: $REAL_ID) : $BOOLEAN_ID
    method Equal(p: $INTEGER_ID) : $BOOLEAN_ID
    method Equal(p: $REAL_ID) : $BOOLEAN_ID
end
"""

private const val REAL_SOURCE_CODE = """
class $REAL_ID extends $ANY_VALUE_ID is
    this(p: $REAL_ID) is end
    this(p: $REAL_ID) is end
    
    var Min : $REAL_ID
    var Max : $REAL_ID
    var Epsilon : $REAL_ID
    
    method toInteger : $INTEGER_ID
    
    method UnaryMinus : $REAL_ID
    
    method Plus(p: $REAL_ID) : $REAL_ID
    method Plus(p: $INTEGER_ID) : $REAL_ID
    method Minus(p: $REAL_ID) : $REAL_ID
    method Minus(p: $INTEGER_ID) : $REAL_ID
    method Mult(p: $REAL_ID) : $REAL_ID
    method Mult(p: $INTEGER_ID) : $REAL_ID
    method Div(p: $INTEGER_ID) : $REAL_ID
    method Div(p: $REAL_ID) : $REAL_ID
    method Rem(p: $INTEGER_ID) : $REAL_ID
end
"""
