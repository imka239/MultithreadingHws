import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class SynchronousQueueMS<E> : SynchronousQueue<E> {
    // TODO head and tail pointers

    private inner class Node {
        val element: AtomicReference<E?> = AtomicReference(null)
        val isSender: AtomicReference<Boolean?> = AtomicReference(null)
        val next: AtomicReference<Node?> = AtomicReference(null)
        val continuation: AtomicReference<Continuation<Any?>?> = AtomicReference(null)
    }

    private val node = Node()
    private var head: AtomicReference<Node> = AtomicReference(node)
    private var tail: AtomicReference<Node> = AtomicReference(node)

    private suspend fun enqueueAndSuspend(tail:  Node, node: Node): Boolean {
        val res = suspendCoroutine<Any?> coroutine@{ cont ->
            node.continuation.set(cont)
            if (tail.next.compareAndSet(null, node)) {
                this.tail.compareAndSet(tail, node)
            } else {
                this.tail.compareAndSet(tail, tail.next.get())
                cont.resume(50)
                return@coroutine
            }
        }
        return res != 50
    }

    override suspend fun send(element: E) {
        //TODO("Implement me!")
        while (true) {
            val head = this.head.get()
            val tail = this.tail.get()

            val node = Node()
            node.element.set(element)
            node.isSender.set(true)

            if (tail == head || tail.isSender.get()!!) {
                if (enqueueAndSuspend(tail, node)) {
                    return
                }
            } else {
                val next = head.next.get() ?: continue
                if (this.head.compareAndSet(head, next)) {
                    next.element.set(element)
                    next.continuation.get()!!.resume(null)
                    return
                }
            }
        }
    }

    override suspend fun receive(): E {
        //TODO("Implement me!")
        while (true) {
            val head = this.head.get()
            val tail = this.tail.get()

            val node = Node()
            node.isSender.set(false)

            if (tail == head || !tail.isSender.get()!!) {
                if (enqueueAndSuspend(tail, node)) {
                    return node.element.get()!!
                }
            } else {
                val next = head.next.get() ?: continue
                if (this.head.compareAndSet(head, next)) {
                    next.continuation.get()!!.resume(null)
                    return head.next.get()!!.element.get()!!
                }
            }

        }
    }
}
