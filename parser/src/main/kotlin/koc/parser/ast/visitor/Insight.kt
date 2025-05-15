package koc.parser.ast.visitor

enum class Insight {
    /**
     * Continue to visit nested nodes
     */
    CONTINUE,

    /**
     * Skip nested nodes, but continue to visit the same level
     */
    SKIP,

    /**
     * Stop visiting the next nodes and exit visitor
     */
    STOP
}