package koc.utils

class UnsupportedException(val feature: String) : CompileException("$feature is not supported") {
}