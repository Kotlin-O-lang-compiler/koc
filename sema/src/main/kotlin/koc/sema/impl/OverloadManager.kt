package koc.sema.impl

import koc.ast.ClassDecl
import koc.ast.ClassType
import koc.ast.ConstructorDecl
import koc.ast.MethodDecl
import koc.ast.Params
import koc.core.Diagnostics
import koc.lex.diag
import koc.parser.ast.Attribute
import koc.parser.ast.Identifier
import koc.parser.size
import koc.sema.diag.ConstructorOverloadFailed
import koc.sema.diag.MethodOverloadFailed

class OverloadManager(val diag: Diagnostics) {
    private val overloadById = hashMapOf<Identifier, HashMap<Identifier, ArrayList<MethodDecl>>>()
    private val ctorOverloadById = hashMapOf<Identifier, ArrayList<ConstructorDecl>>()
    private val classById = hashMapOf<Identifier, ClassDecl>()

    operator fun plusAssign(method: MethodDecl) {
        if (method.isForwardDecl && !method.outerDecl.isBuiltIn) return // proceed only actual declarations
        overload(method.outerDecl, method)
    }

    operator fun plusAssign(ctor: ConstructorDecl) {
        overload(ctor.outerDecl, ctor)
    }

    private fun overload(classDecl: ClassDecl, method: MethodDecl) {
        val classId = classDecl.identifier
        if (classId !in classById) classById[classId] = classDecl

        val candidates = getCandidates(classId, method)

        if (candidates.isNotEmpty()) {
            method.enable(Attribute.BROKEN)
            diag.diag(MethodOverloadFailed(method, candidates), method.signatureWindow)
        }
        overloadById[classId]!![method.identifier]!! += method
    }

    private fun overload(classDecl: ClassDecl, ctor: ConstructorDecl) {
        val classId = classDecl.identifier
        if (classId !in classById) classById[classId] = classDecl

        val candidates = getCandidates(classId, ctor)

        if (candidates.isNotEmpty()) {
            ctor.enable(Attribute.BROKEN)
            diag.diag(ConstructorOverloadFailed(ctor, candidates), ctor.signatureWindow)
        }
        ctorOverloadById[classId]!! += ctor
    }

    private fun getMethodCandidatesByParametersCount(classId: Identifier, methodId: Identifier, params: Int): List<MethodDecl> {
        check(params >= 0)
        return overloadById.getOrPut(classId) { hashMapOf<Identifier, ArrayList<MethodDecl>>() }
            .getOrPut(methodId) { arrayListOf() }.filter { method -> method.params.size == params }
    }

    private fun getCtorCandidatesByParametersCount(classId: Identifier, params: Int): List<ConstructorDecl> {
        check(params >= 0)
        return ctorOverloadById.getOrPut(classId) { ArrayList<ConstructorDecl>() }
            .filter { ctor -> ctor.params.size == params }
    }

    fun getCandidates(classId: Identifier, method: MethodDecl): List<MethodDecl> {
        val prefiltered = getMethodCandidatesByParametersCount(classId, method.identifier, method.params.size)

        return prefiltered.filter { target -> !target.isBroken && areParamsEqual(method.params, target.params) }
    }

    fun getSuitable(classId: Identifier, methodIdentifier: Identifier, params: List<ClassType>): MethodDecl? {
        val prefiltered = getMethodCandidatesByParametersCount(classId, methodIdentifier, params.size)

        val candidates = prefiltered.filter { target -> !target.isBroken && areParamsEqual(params, target.params?.types ?: listOf()) }
        if (candidates.size != 1) return null
        return candidates.first()
    }

    fun getCandidates(classId: Identifier, ctor: ConstructorDecl): List<ConstructorDecl> {
        val prefiltered = getCtorCandidatesByParametersCount(classId, ctor.params.size)

        return prefiltered.filter { target -> !target.isBroken && areParamsEqual(ctor.params, target.params) }
    }

    fun getSuitable(classId: Identifier, params: List<ClassType>): ConstructorDecl? {
        val prefiltered = getCtorCandidatesByParametersCount(classId, params.size)

        val candidates = prefiltered.filter { target -> !target.isBroken && areParamsEqual(params, target.params?.types ?: listOf()) }
        if (candidates.size != 1) return null
        return candidates.first()
    }

    private fun areParamsEqual(first: List<ClassType>, other: List<ClassType>): Boolean {
        if (first.size != other.size) return false

        for (i in first.indices) {
            if (first[i].identifier != other[i].identifier) return false
        }
        return true
    }

    private fun areParamsEqual(first: Params?, other: Params?): Boolean {
        return areParamsEqual(first?.types ?: emptyList(), other?.types ?: listOf())
    }
}
