package koc.driver

import koc.utils.CompileException

abstract class DriverException(override val message: String) : CompileException(message) {
}

class ProgramsNotPassedException : DriverException("Program file(s) not specified as the command line arguments")

class UnknownArgumentException(val argument: String) : DriverException("Unknown command line argument passed: '$argument'")