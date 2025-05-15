package koc.parser.ast.visitor


abstract class AbstractVisitor<T>(
    override val order: Order = Order.TOP_DOWN, override val onBroken: Insight = Insight.SKIP
): Visitor<T> {
    final override var insight: Insight = Insight.CONTINUE
        private set

    protected fun emit(insight: Insight) {
        this.insight = insight
    }

    override fun reset() {
        insight = Insight.CONTINUE
    }

    override fun stop() {
        insight = Insight.STOP
    }
}