class MainKt {
    fun main(args: Array<String>) {
        val que: FCPriorityQueue<Int> = FCPriorityQueue()
        que.add(1)
        print(que.poll())
        print(que.poll())
    }
}