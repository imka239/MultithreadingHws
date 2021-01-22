package dijkstra

import java.util.*
import java.util.concurrent.Phaser
import java.util.concurrent.atomic.AtomicInteger
import kotlin.Comparator
import kotlin.concurrent.thread
import java.util.concurrent.locks.ReentrantLock

private val NODE_DISTANCE_COMPARATOR = Comparator<Node> { o1, o2 -> Integer.compare(o1!!.distance, o2!!.distance) }

class MyQueue(val workers : Int) {
    var allQueue = Array(workers){PriorityQueue(NODE_DISTANCE_COMPARATOR)}
    var lockerQueue = Array(workers) {ReentrantLock()}

    fun add(value : Node) {
        var index = (Math.random() * workers).toInt()
        while (!lockerQueue[index].tryLock()) {
            index = (Math.random() * workers).toInt()
        }
        allQueue[index].add(value)
        //println("gotLock")
        lockerQueue[index].unlock()
        //println("returnedLock")
    }

    fun takeMin() : Node? {
        var index1 = (Math.random() * workers).toInt()
        var index2 = (Math.random() * workers).toInt()
        while (!lockerQueue[index1].tryLock()) {
            index1 = (Math.random() * workers).toInt()
        }
        if (allQueue[index1].isEmpty()) {
            lockerQueue[index1].unlock()
            return null
        }
        val elem1 = allQueue[index1].peek()
        while (!lockerQueue[index2].tryLock()) {
            index2 = (Math.random() * workers).toInt()
        }
        if (allQueue[index2].isEmpty()) {
            allQueue[index1].poll()
            lockerQueue[index2].unlock()
            lockerQueue[index1].unlock()
            return elem1
        }
        val elem2 = allQueue[index2].peek()
        return if (elem1.distance < elem2.distance) {
            allQueue[index1].poll()
            lockerQueue[index1].unlock()
            lockerQueue[index2].unlock()
            elem1
        } else {
            allQueue[index2].poll()
            lockerQueue[index1].unlock()
            lockerQueue[index2].unlock()
            elem2
        }
    }
}

// Returns `Integer.MAX_VALUE` if a path has not been found.
fun shortestPathParallel(start: Node) {
    val workers = Runtime.getRuntime().availableProcessors()
    // The distance to the start node is `0`
    start.distance = 0
    // Create a priority (by distance) queue and add the start node into it
    val q = MyQueue(workers) // TODO replace me with a multi-queue based PQ!
    q.add(start)
    val counter = AtomicInteger(1)
    // Run worker threads and wait until the total work is done
    val onFinish = Phaser(workers + 1) // `arrive()` should be invoked at the end by each worker
    repeat(workers) {
        thread {
            while (counter.get() > 0) {
                //print(counter.get())
                val cur = q.takeMin() ?: continue
                for (e in cur.outgoingEdges) {
                    while (true) {
                        val d1 = e.to.distance
                        val d2 = cur.distance
                        if (d1 > d2 + e.weight) {
                            if (e.to.casDistance(d1, d2 + e.weight)) {
                                counter.incrementAndGet()
                                q.add(e.to)
                                break
                            }
                        } else {
                            break
                        }
                    }
                }
                counter.decrementAndGet()
                //print(counter.get())
            }
            onFinish.arrive()
        }
    }
    onFinish.arriveAndAwaitAdvance()
}