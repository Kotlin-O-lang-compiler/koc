package koc.sema.diag

import koc.ast.ClassDecl
import koc.ast.FieldDecl
import koc.ast.MemberAccessExpr
import koc.ast.MethodBody
import koc.ast.MethodDecl
import koc.ast.RefExpr
import koc.ast.VarDecl
import koc.core.Diagnostics
import koc.lex.Lexer
import koc.lex.fromOptions
import koc.parser.Parser
import koc.parser.fromOptions
import koc.sema.TypeManager
import koc.sema.impl.ReferenceResolver
import koc.sema.performSemaStage
import koc.sema.semaVisitors
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class TestReferenceResolver {
    private lateinit var diag: Diagnostics
    private lateinit var lexer: Lexer
    private lateinit var parser: Parser
    private lateinit var typeManager: TypeManager

    @BeforeEach
    fun before() {
        diag = Diagnostics()
        lexer = Lexer.fromOptions(diag = diag)
        parser = Parser.fromOptions(diag = diag)
        typeManager = TypeManager(Lexer.fromOptions(diag = diag), Parser.fromOptions(diag = diag))
    }

    @Test
    fun `test undefined this`() {
        val code = """
            this
        """.trimIndent()

        val tokens = lexer.lex(code)
        val nodes = listOf(parser.parseRefExpr(tokens))
        assertFalse(diag.hasErrors)

        semaVisitors(typeManager, diag).dropLastWhile { it !is ReferenceResolver }.forEach { stage ->
            performSemaStage(nodes, stage, typeManager)
        }

        assertTrue(diag.hasErrors)
        assertTrue(diag.has<UndefinedReferenceKind>())
        val msg = (diag.diagnostics.last() as UndefinedReference<*>)
        assertNotNull(msg.ref)
        assertIs<RefExpr>(msg.ref)
        assertIs<ClassDecl>(msg.ref.ref)
        assertTrue(with(typeManager) { (msg.ref.ref as ClassDecl).isInvalid })
    }

    @Test
    fun `test explicit super type`() {
        val code = """
            class A extends B is end
            class B is end
        """.trimIndent()

        val tokens = lexer.lex(code)
        val nodes = parser.parseNodes(tokens)
        assertFalse(diag.hasErrors)

        val a = nodes[0] as ClassDecl
        val b = nodes[1] as ClassDecl

        semaVisitors(typeManager, diag).dropLastWhile { it !is ReferenceResolver }.forEach { stage ->
            performSemaStage(nodes, stage, typeManager)
        }

        assertFalse(diag.hasErrors)

        assertNull(b.superTypeRef)
        assertNotNull(a.superTypeRef)
        assertNotNull(a.superTypeRef!!.ref)
        assertEquals(b.identifier, a.superTypeRef!!.ref!!.identifier)
        assertFalse(a.superTypeRef!!.isBroken)
    }

    @Test
    fun `test this reference`() {
        val code = """
            class A is
                var x: 0
                method x is end
                var y: this.x
            end
        """.trimIndent()

        val tokens = lexer.lex(code)
        val nodes = parser.parseNodes(tokens)
        assertFalse(diag.hasErrors)

        val a = nodes[0] as ClassDecl
        val x = a.body.members.first() as FieldDecl
        val y = a.body.members.last() as FieldDecl
        val thisRef = (y.varDecl.initializer as MemberAccessExpr).left as RefExpr
        val thisX = (y.varDecl.initializer as MemberAccessExpr).member as RefExpr

        semaVisitors(typeManager, diag).dropLastWhile { it !is ReferenceResolver }.forEach { stage ->
            performSemaStage(nodes, stage, typeManager)
        }

        assertFalse(diag.hasErrors)

        assertNotNull(thisRef.ref)
        assertEquals(a.identifier, thisRef.ref!!.identifier)
        assertSame(a, thisRef.ref!!)

        assertNotNull(thisX.ref)
        assertEquals(x.identifier, thisX.ref!!.identifier)
        assertSame(x, thisX.ref!!)
    }

    @Test
    fun `test field redefinition`() {
        val code = """
            class A is
                var x: 0
                var x: true
            end
        """.trimIndent()

        val tokens = lexer.lex(code)
        val nodes = parser.parseNodes(tokens)
        assertFalse(diag.hasErrors)

        val a = nodes[0] as ClassDecl
        val x1 = a.body.members.first() as FieldDecl
        val x2 = a.body.members.last() as FieldDecl

        semaVisitors(typeManager, diag).dropLastWhile { it !is ReferenceResolver }.forEach { stage ->
            performSemaStage(nodes, stage, typeManager)
        }

        assertTrue(diag.hasErrors)
        diag.has<DeclRedefinitionKind>()
        val msg = diag.diagnostics.first() as DeclRedefinition
        assertSame(x2, msg.decl)
        assertSame(x1, msg.previousDecl)
    }

    @Test
    fun `test var redefinition`() {
        val code = """
            class A is
                method foo is
                    var a: 0
                    var x: 0
                    var y: true
                    var z: y
                    var variable: z
                    var x: true
                end
            end
        """.trimIndent()

        val tokens = lexer.lex(code)
        val nodes = parser.parseNodes(tokens)
        assertFalse(diag.hasErrors)

        val a = nodes[0] as ClassDecl
        val foo = a.body.members.first() as MethodDecl
        val fooBody = foo.body as MethodBody.MBody
        val x1 = fooBody.body.nodes[1] as VarDecl
        val x2 = fooBody.body.nodes.last() as VarDecl

        semaVisitors(typeManager, diag).dropLastWhile { it !is ReferenceResolver }.forEach { stage ->
            performSemaStage(nodes, stage, typeManager)
        }

        assertTrue(diag.hasErrors)
        diag.has<DeclRedefinitionKind>()
        val msg = diag.diagnostics.first() as DeclRedefinition
        assertSame(x2, msg.decl)
        assertSame(x1, msg.previousDecl)
    }

    @Test
    fun `test field by var redefinition`() {
        val code = """
            class A is
                var x: true
                method foo is
                    var a: 0
                    var x: 0
                    var y: true
                    var z: y
                    var variable: z
                end
            end
        """.trimIndent()

        val tokens = lexer.lex(code)
        val nodes = parser.parseNodes(tokens)
        assertFalse(diag.hasErrors)

        val a = nodes[0] as ClassDecl
        val foo = a.body.members.last() as MethodDecl
        val fooBody = foo.body as MethodBody.MBody
        val x1 = a.body.members.first() as FieldDecl
        val x2 = fooBody.body.nodes[1] as VarDecl

        semaVisitors(typeManager, diag).dropLastWhile { it !is ReferenceResolver }.forEach { stage ->
            performSemaStage(nodes, stage, typeManager)
        }

        assertTrue(diag.hasErrors)
        diag.has<DeclRedefinitionKind>()
        val msg = diag.diagnostics.first() as DeclRedefinition
        assertSame(x2, msg.decl)
        assertSame(x1, msg.previousDecl)
    }

    @Test
    fun `test field by param redefinition`() {
        val code = """
            class A is
                var x: true
                method foo(x: A) is
                end
            end
        """.trimIndent()

        val tokens = lexer.lex(code)
        val nodes = parser.parseNodes(tokens)
        assertFalse(diag.hasErrors)

        val a = nodes[0] as ClassDecl
        val foo = a.body.members.last() as MethodDecl
        val x1 = a.body.members.first() as FieldDecl
        val x2 = foo.params!!.params.first()

        semaVisitors(typeManager, diag).dropLastWhile { it !is ReferenceResolver }.forEach { stage ->
            performSemaStage(nodes, stage, typeManager)
        }

        assertTrue(diag.hasErrors)
        diag.has<DeclRedefinitionKind>()
        val msg = diag.diagnostics.first() as DeclRedefinition
        assertSame(x2, msg.decl)
        assertSame(x1, msg.previousDecl)
    }

    @Test
    fun `test param by var redefinition`() {
        val code = """
            class A is
                method foo(x: A) is
                    var x: true
                end
            end
        """.trimIndent()

        val tokens = lexer.lex(code)
        val nodes = parser.parseNodes(tokens)
        assertFalse(diag.hasErrors)

        val a = nodes[0] as ClassDecl
        val foo = a.body.members.last() as MethodDecl
        val x1 = foo.params!!.params.first()
        val x2 = (foo.body as MethodBody.MBody).body.nodes[0] as VarDecl

        semaVisitors(typeManager, diag).dropLastWhile { it !is ReferenceResolver }.forEach { stage ->
            performSemaStage(nodes, stage, typeManager)
        }

        assertTrue(diag.hasErrors)
        diag.has<DeclRedefinitionKind>()
        val msg = diag.diagnostics.first() as DeclRedefinition
        assertSame(x2, msg.decl)
        assertSame(x1, msg.previousDecl)
    }

    @Test
    fun `test class by field redefinition`() {
        val code = """
            class A is
                var x: 0
            end
            class x is end
        """.trimIndent()

        val tokens = lexer.lex(code)
        val nodes = parser.parseNodes(tokens)
        assertFalse(diag.hasErrors)

        val a = nodes[0] as ClassDecl
        val x1 = nodes[1] as ClassDecl
        val x2 = a.body.members.first() as FieldDecl

        semaVisitors(typeManager, diag).dropLastWhile { it !is ReferenceResolver }.forEach { stage ->
            performSemaStage(nodes, stage, typeManager)
        }

        assertTrue(diag.hasErrors)
        diag.has<DeclRedefinitionKind>()
        val msg = diag.diagnostics.first() as DeclRedefinition
        assertSame(x2, msg.decl)
        assertSame(x1, msg.previousDecl)
    }

    @Test
    fun `test undefined field initializer`() {
        val code = """
            class A is
                var x: y
            end
        """.trimIndent()

        val tokens = lexer.lex(code)
        val nodes = parser.parseNodes(tokens)
        assertFalse(diag.hasErrors)

        val a = nodes[0] as ClassDecl
        val x = a.body.members.first() as FieldDecl
        val y = (x.varDecl.initializer as RefExpr)

        semaVisitors(typeManager, diag).dropLastWhile { it !is ReferenceResolver }.forEach { stage ->
            performSemaStage(nodes, stage, typeManager)
        }

        assertTrue(diag.hasErrors)
        diag.has<UndefinedReferenceKind>()
        val msg = diag.diagnostics.first() as UndefinedReference<*>
        assertSame(y, msg.ref)
    }


    @Test
    fun `test field literal type`() {
        val code = """
            class A is
                var x: 0
                var y: true
            end
        """.trimIndent()

        val tokens = lexer.lex(code)
        val nodes = parser.parseNodes(tokens)
        assertFalse(diag.hasErrors)

        val a = nodes[0] as ClassDecl
        val x = a.body.members.first() as FieldDecl
        val y = a.body.members.last() as FieldDecl

        semaVisitors(typeManager, diag).dropLastWhile { it !is ReferenceResolver }.forEach { stage ->
            performSemaStage(nodes, stage, typeManager)
        }

        assertFalse(diag.hasErrors)
        assertSame(typeManager.intType, x.rootType)
        assertSame(typeManager.intType, x.type)
        assertSame(typeManager.boolType, y.rootType)
        assertSame(typeManager.boolType, y.type)
    }

    @Test
    fun `test field dependency type`() {
        val code = """
            class A is
                var x: 0
                var y: x
            end
        """.trimIndent()

        val tokens = lexer.lex(code)
        val nodes = parser.parseNodes(tokens)
        assertFalse(diag.hasErrors)

        val a = nodes[0] as ClassDecl
        val x = a.body.members.first() as FieldDecl
        val y = a.body.members.last() as FieldDecl

        semaVisitors(typeManager, diag).dropLastWhile { it !is ReferenceResolver }.forEach { stage ->
            performSemaStage(nodes, stage, typeManager)
        }

        assertFalse(diag.hasErrors)
        assertSame(typeManager.intType, x.rootType)
        assertSame(typeManager.intType, x.type)
        assertSame(typeManager.intType, y.rootType)
        assertSame(typeManager.intType, y.type)
    }

    @Test
    fun `test field reversed dependency type`() {
        val code = """
            class A is
                var x: y
                var y: 10
            end
        """.trimIndent()

        val tokens = lexer.lex(code)
        val nodes = parser.parseNodes(tokens)
        assertFalse(diag.hasErrors)

        val a = nodes[0] as ClassDecl
        val x = a.body.members.first() as FieldDecl
        val yref = x.varDecl.initializer as RefExpr
        val y = a.body.members.last() as FieldDecl

        semaVisitors(typeManager, diag).dropLastWhile { it !is ReferenceResolver }.forEach { stage ->
            performSemaStage(nodes, stage, typeManager)
        }

        assertTrue(diag.hasErrors)
        val msg = diag.diagnostics.first() as UndefinedReference<*>
        assertSame(msg.ref, yref)
        assertTrue(yref.isBroken)
        assertTrue(with(typeManager) { yref.isBroken })
        assertTrue(with(typeManager) { x.type.isInvalid })
        assertTrue(with(typeManager) { x.rootType.isInvalid })

        assertSame(typeManager.intType, y.rootType)
        assertSame(typeManager.intType, y.type)
    }

    @Test
    fun `test field cross dependency type`() {
        val code = """
            class A is
                var x: y
                var y: x
            end
        """.trimIndent()

        val tokens = lexer.lex(code)
        val nodes = parser.parseNodes(tokens)
        assertFalse(diag.hasErrors)

        val a = nodes[0] as ClassDecl
        val x = a.body.members.first() as FieldDecl
        val y = a.body.members.last() as FieldDecl

        semaVisitors(typeManager, diag).dropLastWhile { it !is ReferenceResolver }.forEach { stage ->
            performSemaStage(nodes, stage, typeManager)
        }

        assertTrue(diag.hasErrors)

//        assertSame(x, x.type.field)
//        assertSame(typeManager.intType, y.rootType)
//        assertSame(y, y.type.field)
    }
}