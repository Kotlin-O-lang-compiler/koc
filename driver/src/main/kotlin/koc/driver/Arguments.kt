package koc.driver

sealed class Arguments() {
    companion object : ArgumentName {
        override val name = ""
    }

    data object DumpTokens : Arguments(), ArgumentName {
        override val name = "--dump-tokens"
    }


    interface ArgumentName {
        val name: String
    }
}