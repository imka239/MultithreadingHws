import java.util.function.Supplier

/**
 * @author :TODO: Gnatyuk Dmitriy
 */
class Solution : AtomicCounter {
    private val root = Node(0)
    private val ref: ThreadLocal<Node> = ThreadLocal.withInitial { root }
    // объявите здесь нужные вам поля
    override fun getAndAdd(x: Int): Int {
        var res: Int
        var expected: Node
        var newNode: Node
        do {
            res = ref.get().value
            newNode = Node(res + x)
            expected = ref.get().next.decide(newNode)
            ref.set(expected)
        } while (expected !== newNode)
        return res
    }

    // вам наверняка потребуется дополнительный класс
    private class Node internal constructor(val value: Int) : Supplier<Any?> {
        val next: Consensus<Node> = Consensus()
        override fun get(): Node? {
            return this
        }

    }
}