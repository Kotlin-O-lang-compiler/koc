package koc.core

class UnsupportedException(val feature: String) : CompileException("$feature is not supported") {
}