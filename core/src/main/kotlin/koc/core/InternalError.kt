package koc.core

class InternalError(override val message: String) : Exception(message) {
}