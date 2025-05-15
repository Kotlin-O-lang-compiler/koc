package koc.utils

class InternalError(override val message: String) : Exception(message) {
}