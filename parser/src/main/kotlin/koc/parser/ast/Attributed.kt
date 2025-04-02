package koc.parser.ast

interface Attributed {
    val attrs: Set<Attribute>
    fun enable(attr: Attribute)
    fun disable(attr: Attribute)
}