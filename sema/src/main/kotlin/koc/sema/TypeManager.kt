package koc.sema

import koc.ast.ClassBody
import koc.ast.ClassDecl
import koc.ast.ClassType
import koc.core.InternalError
import koc.core.Position
import koc.lex.Lexer
import koc.lex.Token
import koc.lex.TokenKind
import koc.parser.Parser
import koc.parser.ast.Attribute
import koc.parser.ast.Identifier
import koc.parser.impl.ParseScope
import koc.sema.TypeManager.Companion.ANY_REF_ID
import koc.sema.TypeManager.Companion.ANY_VALUE_ID
import koc.sema.TypeManager.Companion.BOOLEAN_ID
import koc.sema.TypeManager.Companion.CLASS_ID
import koc.sema.TypeManager.Companion.INTEGER_ID
import koc.sema.TypeManager.Companion.REAL_ID

class TypeManager(lexer: Lexer, parser: Parser) {
    val classType: ClassType
        get() = classDecl.type

    val anyValueType: ClassType
        get() = anyValueDecl.type

    val anyRefType: ClassType
        get() = anyRefDecl.type

    val intType: ClassType
        get() = intDecl.type

    val boolType: ClassType
        get() = boolDecl.type

    val realType: ClassType
        get() = realDecl.type

    val invalidType: ClassType
        get() = ClassType(invalidDecl, null)

    private val userDefinitions = hashMapOf<String, ClassDecl>()

    fun getUserDefinition(name: String): ClassDecl = userDefinitions[name]!!

    fun hasUserDefinition(name: String): Boolean = name in userDefinitions

    fun learn(userDefinition: ClassDecl) {
        userDefinitions[userDefinition.identifier.value] = userDefinition
    }

    fun hasDefinition(name: String): Boolean = name in userDefinitions || name in builtInDecls

    fun hasDefinition(name: Identifier) = hasDefinition(name.value)

    fun getDefinition(name: String): ClassDecl = builtInDecls[name] ?: getUserDefinition(name)

    fun getDefinition(name: Identifier) = getDefinition(name.value)

    private val builtInDecls by lazy {
        mutableMapOf<String, ClassDecl>(
            classDecl.identifier.value to classDecl,
            anyValueDecl.identifier.value to anyValueDecl,
            anyRefDecl.identifier.value to anyRefDecl,
            intDecl.identifier.value to intDecl,
            boolDecl.identifier.value to boolDecl,
            realDecl.identifier.value to realDecl,
//            invalidDecl.identifier.value to invalidDecl
        )
    }

    val builtInDeclarations: List<ClassDecl>
        get() = builtInDecls.values.toList()

    private val types by lazy {
        mutableMapOf<String, ClassType>(
            classType.identifier.value to classType,
            anyValueType.identifier.value to anyValueType,
            anyRefType.identifier.value to anyRefType,
            intType.identifier.value to intType,
            boolType.identifier.value to boolType,
            realType.identifier.value to realType,
            invalidType.identifier.value to invalidType
        )
    }

    fun getType(name: String): ClassType = types[name] ?: invalidType

    fun getType(name: Identifier) = getType(name.value)

    fun hasType(name: String): Boolean = name in types

    fun hasType(name: Identifier) = hasType(name.value)

    internal fun learn(type: ClassType) {
        require(type.classDecl.identifier.value in userDefinitions)
        if (type.identifier.value in types) throw InternalError("Possible type redefinition ${type.identifier}")
        type.classDecl.specifyType(type)
        types[type.identifier.value] = type
    }

    val classDecl = lexer.lex(CLASS_SOURCE_CODE, STD_FILE_NAME).let { tokens ->
        parser.parseClassDecl(tokens).apply {
            enable(Attribute.BUILTIN)
            specifyType(ClassType(this, null))
            specifyScope(ParseScope.topLevel)
            specifyTokens(tokens)
        }
    }

    val anyValueDecl = lexer.lex(ANY_VALUE_SOURCE_CODE, STD_FILE_NAME).let { tokens ->
        parser.parseClassDecl(tokens).apply {
            enable(Attribute.BUILTIN)
            specifyType(ClassType(this, classType))
            specifyScope(ParseScope.topLevel)
            specifyTokens(tokens)
        }
    }

    val anyRefDecl = lexer.lex(ANY_REF_SOURCE_CODE, STD_FILE_NAME).let { tokens ->
        parser.parseClassDecl(tokens).apply {
            enable(Attribute.BUILTIN)
            specifyType(ClassType(this, classType))
            specifyScope(ParseScope.topLevel)
            specifyTokens(tokens)
        }
    }

    val intDecl = lexer.lex(INTEGER_SOURCE_CODE, STD_FILE_NAME).let { tokens ->
        parser.parseClassDecl(tokens).apply {
            enable(Attribute.BUILTIN)
            specifyType(ClassType(this, anyValueType))
            specifyScope(ParseScope.topLevel)
            specifyTokens(tokens)
        }
    }

    val boolDecl = lexer.lex(BOOLEAN_SOURCE_CODE, STD_FILE_NAME).let { tokens ->
        parser.parseClassDecl(tokens).apply {
            enable(Attribute.BUILTIN)
            specifyType(ClassType(this, anyValueType))
            specifyScope(ParseScope.topLevel)
            specifyTokens(tokens)
        }
    }

    val realDecl = lexer.lex(REAL_SOURCE_CODE, STD_FILE_NAME).let { tokens ->
        parser.parseClassDecl(tokens).apply {
            enable(Attribute.BUILTIN)
            specifyType(ClassType(this, anyValueType))
            specifyScope(ParseScope.topLevel)
            specifyTokens(tokens)
        }
    }

    val invalidDecl = ClassDecl(
        Token.invalid,
        Token(INVALID_ID, TokenKind.IDENTIFIER, Position.fake),
        body = ClassBody(Token.invalid, Token.invalid)
    ).apply {
        specifyType(ClassType(this, null))
        specifyScope(ParseScope.topLevel)
        enable(Attribute.BUILTIN)
        enable(Attribute.BROKEN)
    }

    val ClassDecl.isClass: Boolean get() = identifier.value == CLASS_ID
    val ClassDecl.isAnyValue: Boolean get() = identifier.value == ANY_VALUE_ID
    val ClassDecl.isAnyRef: Boolean get() = identifier.value == ANY_REF_ID
    val ClassDecl.isInt: Boolean get() = identifier.value == INTEGER_ID
    val ClassDecl.isReal: Boolean get() = identifier.value == REAL_ID
    val ClassDecl.isBool: Boolean get() = identifier.value == BOOLEAN_ID
    val ClassDecl.isInvalid: Boolean get() = identifier.value == INVALID_ID

    val ClassType.isClass: Boolean get() = identifier.value == CLASS_ID
    val ClassType.isAnyValue: Boolean get() = identifier.value == ANY_VALUE_ID
    val ClassType.isAnyRef: Boolean get() = identifier.value == ANY_REF_ID
    val ClassType.isInt: Boolean get() = identifier.value == INTEGER_ID
    val ClassType.isReal: Boolean get() = identifier.value == REAL_ID
    val ClassType.isBool: Boolean get() = identifier.value == BOOLEAN_ID
    val ClassType.isInvalid: Boolean get() = identifier.value == INVALID_ID

    companion object {
        internal val fakeStdPos = Position(0u, 0u, "std")
        const val CLASS_ID = "Class"
        const val ANY_VALUE_ID = "AnyValue"
        const val ANY_REF_ID = "AnyRef"
        const val INTEGER_ID = "Integer"
        const val REAL_ID = "Real"
        const val BOOLEAN_ID = "Boolean"
        const val INVALID_ID = "\$invalid"
        val builtinTypes = arrayOf(CLASS_ID, ANY_VALUE_ID, INTEGER_ID, REAL_ID, BOOLEAN_ID)

        private const val STD_FILE_NAME = "std.ol"
    }
}

private const val CLASS_SOURCE_CODE = """
class $CLASS_ID is end
"""

private const val ANY_VALUE_SOURCE_CODE = """
class $ANY_VALUE_ID extends $CLASS_ID is end
"""

private const val ANY_REF_SOURCE_CODE = """
class $ANY_REF_ID extends $CLASS_ID is end
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
    this(p: $INTEGER_ID) is end
    
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
