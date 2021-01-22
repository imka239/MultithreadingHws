import kotlinx.atomicfu.*
import java.lang.IllegalArgumentException

private val DONE = Any()
private class TOMOVE(val value: Any?)

class DynamicArrayImpl<E> : DynamicArray<E> {
    private val core : AtomicRef<Core?> = atomic(Core(INITIAL_CAPACITY))
    private val sz = atomic(0)

    override fun get(index: Int): E {
        if (index >= sz.value) {
            throw IllegalArgumentException("index too big")
        }
        while (true) {
            val copyCore = core.value
            val ans = copyCore!!.get(index)
            if (ans == DONE) {
                continue
            } else {
                return ans as E
            }
        }
    }

    override fun put(index: Int, element: E) {
        if (index >= sz.value) {
            throw IllegalArgumentException("index too big")
        }
        while (true) {
            val copyCore = core.value
            val ans = copyCore!!.put(index, element)
            if (!ans) {
                continue
            } else {
                return
            }
        }
        TODO("Not yet implemented")
    }

    override fun pushBack(element: E) {
        while (true) {
            val copyCore = core.value
            var sz_photo = sz.value
            if (sz_photo < copyCore!!.capacity) {
                if (copyCore.pushBack(sz_photo, element)) {
                    while (!sz.compareAndSet(sz_photo, sz_photo + 1)) {
                        sz_photo = sz.value
                    }
                    return
                } else {
                    continue
                }
            } else {
                if (copyCore.next.compareAndSet(null, Core(copyCore.capacity * 2))) {
                    for (i in 0 until copyCore.capacity) {
                        while (true) {
                            val value = copyCore.get(i)
                            if (copyCore.array[i].compareAndSet(value, TOMOVE(value))) {
                                break
                            }
                        }
                    }
                    for (i in 0 until copyCore.capacity) {
                        while (true) {
                            val value = copyCore.array[i].value
                            if (value is TOMOVE) {
                                copyCore.next.value!!.put(i, value.value)
                            }
                            if (copyCore.array[i].compareAndSet(value, DONE)) {
                                break
                            }
                        }
                    }
                    core.compareAndSet(copyCore, copyCore.next.value)
                }
            }
        }
        TODO("Not yet implemented")
    }

    override val size: Int get() {
        return sz.value
    }
}

class Core(val capacity: Int) {
    val next = atomic<Core?>(null)
    val array = atomicArrayOfNulls<Any?>(capacity)

    fun get(index: Int): Any? {
        val copy = array[index].value
        if (copy is TOMOVE) {
            return copy.value
        } else {
            return copy
        }
    }

    fun put(index: Int, elem : Any?) : Boolean {
        val copy = array[index].value
        if (copy is TOMOVE) {
            next.value!!.put(index, copy.value)
            return false
        }
        if (copy == DONE) {
            return next.value!!.put(index, elem)
        } else {
            return array[index].compareAndSet(copy, elem)
        }
    }

    fun pushBack(index: Int, elem : Any?) : Boolean {
        val copy = array[index].value
        if (copy is TOMOVE) {
            next.value!!.put(index, copy.value)
            return false
        }
        if (copy == DONE) {
            return next.value!!.pushBack(index, elem)
        } else {
            if (copy == null) {
                if (array[index].compareAndSet(null, elem)) {
                    return true
                }
                return false
            } else {
                return false
            }
        }
    }
}

private const val INITIAL_CAPACITY = 1 // DO NOT CHANGE ME