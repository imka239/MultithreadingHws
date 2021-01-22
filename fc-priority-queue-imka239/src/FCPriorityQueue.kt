import kotlinx.atomicfu.AtomicArray
import kotlinx.atomicfu.atomicArrayOfNulls
import java.util.*
import java.util.concurrent.locks.ReentrantLock

class FCPriorityQueue<E : Comparable<E>> {
    private val sz = 4
    private val q = PriorityQueue<E>()
    private val lock = ReentrantLock()
    private val rnd = Random()
    private val fc_array: AtomicArray<Op<E?>?> = atomicArrayOfNulls(sz)

    fun help() {
        for (i in 0 until sz) {
            var nextToWrite : E? = null
            if (!q.isEmpty()) {
                nextToWrite = q.peek()
            }
            val obj = fc_array[i].value ?: continue
            if (obj.enqDeqPeek == 1 && obj.doneOrNot == 0) {
                fc_array[i].compareAndSet(obj, Op(nextToWrite, 1, 1))
            }
            if (obj.enqDeqPeek == 0 && obj.doneOrNot == 0) {
                if (fc_array[i].compareAndSet(obj, Op(nextToWrite, 0, 1))) {
                    if (!q.isEmpty()) {
                        q.poll()
                    }
                }
            }
            if (obj.enqDeqPeek == -1 && obj.doneOrNot == 0) {
                if (fc_array[i].compareAndSet(obj, Op(null, -1, 1))) {
                    q.add(obj.maybeOp)
                }
            }
        }
    }

    /**
     * Retrieves the element with the highest priority
     * and returns it as the result of this function;
     * returns `null` if the queue is empty.
     */
    fun poll(): E? {
        while (!lock.tryLock()) {
            val ind = (rnd.nextInt() % sz + sz) % sz
            if (fc_array[ind].compareAndSet(null, Op(null, 0, 0))) {
                //sleep(100)
                while(true) {
                    val ans = fc_array[ind].value
                    if (ans != null) {
                        if (ans.doneOrNot == 1) {
                            val toRet = ans.maybeOp
                            if (fc_array[ind].compareAndSet(ans, null)) {
                                return toRet
                            } else {
                                assert(false)
                            }
                        } else {
                            if (fc_array[ind].compareAndSet(ans, null)) {
                                break
                            } else {
                                continue
                            }
                        }
                    } else {
                        assert(false)
                    }
                }
            }
        }
        var ans : E? = null
        if (!q.isEmpty()) {
            ans = q.poll()
        }
        help()
        lock.unlock()
        return ans
    }

    /**
     * Returns the element with the highest priority
     * or `null` if the queue is empty.
     */
    fun peek(): E? {
        while (!lock.tryLock()) {
            val ind = (rnd.nextInt() % sz + sz) % sz
            if (fc_array[ind].compareAndSet(null, Op(null, 1, 0))) {
                //sleep(100)
                while(true) {
                    val ans = fc_array[ind].value
                    if (ans != null) {
                        if (ans.doneOrNot == 1) {
                            val toRet = ans.maybeOp
                            if (fc_array[ind].compareAndSet(ans, null)) {
                                return toRet
                            } else {
                                assert(false)
                            }
                        } else {
                            if (fc_array[ind].compareAndSet(ans, null)) {
                                break
                            } else {
                                continue
                            }
                        }
                    } else {
                        assert(false)
                    }
                }
            }
        }
        var ans : E? = null
        if (!q.isEmpty()) {
            ans = q.peek()
        }
        help()
        lock.unlock()
        return ans
    }

    /**
     * Adds the specified element to the queue.
     */
    fun add(element: E) {
        while (!lock.tryLock()) {
            val ind = (rnd.nextInt() % sz + sz) % sz
            if (fc_array[ind].compareAndSet(null, Op(element, -1, 0))) {
                //sleep(100)
                while (true) {
                    val ans = fc_array[ind].value
                    if (ans != null) {
                        if (ans.doneOrNot == 1) {
                            if (fc_array[ind].compareAndSet(ans, null)) {
                                return
                            } else {
                                assert(false)
                            }
                        } else {
                            if (fc_array[ind].compareAndSet(ans, null)) {
                                break
                            } else {
                                continue
                            }
                        }
                    } else {
                        assert(false)
                    }
                }
            }
        }
        q.add(element)
        help()
        lock.unlock()
    }

    class Op<E>(value: E?, val enqDeqPeek : Int, val doneOrNot:Int) {
        val maybeOp = value
    }
}