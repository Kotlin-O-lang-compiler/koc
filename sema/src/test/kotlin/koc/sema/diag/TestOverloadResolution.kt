package koc.sema.diag

import jdk.jshell.Diag
import koc.ast.ClassDecl
import koc.core.Diagnostics
import koc.lex.Lexer
import koc.lex.fromOptions
import koc.parser.Parser
import koc.parser.fromOptions
import koc.sema.TypeManager
import koc.sema.impl.OverloadValidatorVisitor
import koc.sema.performSemaStage
import koc.sema.semaVisitors
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class TestOverloadResolution {
    private lateinit var diag: Diagnostics
    private lateinit var lexer: Lexer
    private lateinit var parser: Parser
    private lateinit var typeManager: TypeManager

    @BeforeEach
    fun before() {
        diag = Diagnostics()
        lexer = Lexer.fromOptions(diag = diag)
        parser = Parser.fromOptions(diag = diag)
        typeManager = TypeManager(Lexer.fromOptions(diag = Diagnostics()), Parser.fromOptions(diag = Diagnostics()))
    }

    @Test
    fun `test forward definition`() {
        val code = """
            class A is
                method foo
                method foo is end
                method x: A is
                    return this
                end
                method xxx is
                    var tmp: x().foo()
                end
            end
        """.trimIndent()

        val tokens = lexer.lex(code)
        val nodes = parser.parseNodes(tokens)
        assertFalse(diag.hasErrors)

        semaVisitors(typeManager, diag).dropLastWhile { it !is OverloadValidatorVisitor }.forEach { stage ->
            performSemaStage(nodes, stage, typeManager)
        }

        assertFalse(diag.hasErrors)
    }

    @Test
    fun `test bad overload no params`() {
        val code = """
            class A is
                method foo is end
                method goo is end
                method foo() is end
            end
        """.trimIndent()

        val tokens = lexer.lex(code)
        val nodes = parser.parseNodes(tokens)
        assertFalse(diag.hasErrors)

        val a = nodes[0] as ClassDecl
        val foo1 = a.methods.first()
        val foo2 = a.methods.last()

        semaVisitors(typeManager, diag).dropLastWhile { it !is OverloadValidatorVisitor }.forEach { stage ->
            performSemaStage(nodes, stage, typeManager)
        }

        assertTrue(diag.hasErrors)
        assertTrue(diag.has<OverloadResolutionFailedKind>())
        val msg = diag.diagnostics.first() as MethodOverloadFailed
        assertSame(foo2, msg.method)
        assertEquals(1, msg.candidates.size)
        assertSame(foo1, msg.candidates.first())
    }

    @Test
    fun `test bad overload one param`() {
        val code = """
            class A is
                method foo(a: Class) is end
                method goo is end
                method foo(cls: Class) is end
            end
        """.trimIndent()

        val tokens = lexer.lex(code)
        val nodes = parser.parseNodes(tokens)
        assertFalse(diag.hasErrors)

        val a = nodes[0] as ClassDecl
        val foo1 = a.methods.first()
        val foo2 = a.methods.last()

        semaVisitors(typeManager, diag).dropLastWhile { it !is OverloadValidatorVisitor }.forEach { stage ->
            performSemaStage(nodes, stage, typeManager)
        }

        assertTrue(diag.hasErrors)
        assertTrue(diag.has<OverloadResolutionFailedKind>())
        val msg = diag.diagnostics.first() as MethodOverloadFailed
        assertSame(foo2, msg.method)
        assertEquals(1, msg.candidates.size)
        assertSame(foo1, msg.candidates.first())
    }

    @Test
    fun `test bad overload params`() {
        val code = """
            class A is
                method foo(a: Class, b: Integer, c: Boolean, d: A) is end
                method goo is end
                method foo(cls: Class, b: Integer, c: Boolean, a: A) is end
            end
        """.trimIndent()

        val tokens = lexer.lex(code)
        val nodes = parser.parseNodes(tokens)
        assertFalse(diag.hasErrors)

        val a = nodes[0] as ClassDecl
        val foo1 = a.methods.first()
        val foo2 = a.methods.last()

        semaVisitors(typeManager, diag).dropLastWhile { it !is OverloadValidatorVisitor }.forEach { stage ->
            performSemaStage(nodes, stage, typeManager)
        }

        assertTrue(diag.hasErrors)
        assertTrue(diag.has<OverloadResolutionFailedKind>())
        val msg = diag.diagnostics.first() as MethodOverloadFailed
        assertSame(foo2, msg.method)
        assertEquals(1, msg.candidates.size)
        assertSame(foo1, msg.candidates.first())
    }

    @Test
    fun `test constructor bad overload no params`() {
        val code = """
            class A is
                this() is end
                method goo is end
                this(a: Integer) is end
                this(b: Class) is end
                this is end
            end
        """.trimIndent()

        val tokens = lexer.lex(code)
        val nodes = parser.parseNodes(tokens)
        assertFalse(diag.hasErrors)

        val a = nodes[0] as ClassDecl
        val ctor1 = a.constructors.first()
        val ctor2 = a.constructors.last()

        semaVisitors(typeManager, diag).dropLastWhile { it !is OverloadValidatorVisitor }.forEach { stage ->
            performSemaStage(nodes, stage, typeManager)
        }

        assertTrue(diag.hasErrors)
        assertTrue(diag.has<OverloadResolutionFailedKind>())
        val msg = diag.diagnostics.first() as ConstructorOverloadFailed
        assertSame(ctor2, msg.ctor)
        assertEquals(1, msg.candidates.size)
        assertSame(ctor1, msg.candidates.first())
    }

    @Test
    fun `test constructor bad overload params`() {
        val code = """
            class A is
                this(a: Class, b: Integer, c: Boolean, d: A) is end
                method goo is end
                this(cls: Class, b: Integer, c: Boolean, a: A) is end
                this(a: Integer) is end
            end
        """.trimIndent()

        val tokens = lexer.lex(code)
        val nodes = parser.parseNodes(tokens)
        assertFalse(diag.hasErrors)

        val a = nodes[0] as ClassDecl
        val ctor1 = a.constructors.first()
        val ctor2 = a.constructors[1]

        semaVisitors(typeManager, diag).dropLastWhile { it !is OverloadValidatorVisitor }.forEach { stage ->
            performSemaStage(nodes, stage, typeManager)
        }

        assertTrue(diag.hasErrors)
        assertTrue(diag.has<OverloadResolutionFailedKind>())
        val msg = diag.diagnostics.first() as ConstructorOverloadFailed
        assertSame(ctor2, msg.ctor)
        assertEquals(1, msg.candidates.size)
        assertSame(ctor1, msg.candidates.first())
    }

    @Test
    fun `test param redefinition`() {
        val code = """
            class A is
                method foo(a: Integer, b: Real, a: A) is end
            end
        """.trimIndent()

        val tokens = lexer.lex(code)
        val nodes = parser.parseNodes(tokens)
        assertFalse(diag.hasErrors)

        val a = nodes[0] as ClassDecl
        val foo = a.methods.first()
        val a1 = foo.params!![0]
        val a2 = foo.params!![2]

        semaVisitors(typeManager, diag).dropLastWhile { it !is OverloadValidatorVisitor }.forEach { stage ->
            performSemaStage(nodes, stage, typeManager)
        }

        assertTrue(diag.hasErrors)
        assertTrue(diag.has<DeclRedefinitionKind>())
        val msg = diag.diagnostics.first() as DeclRedefinition
        assertSame(a2, msg.decl)
        assertSame(a1, msg.previousDecl)
    }
}