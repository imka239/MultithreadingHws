package msqueue;

import kotlinx.atomicfu.AtomicRef;

public class MSQueue implements Queue {
    private class Node {
        final int x;
        AtomicRef<Node> next;

        Node(int x, Node next) {
            this.next = new AtomicRef<>(next);
            this.x = x;
        }
    }

    private AtomicRef<Node> head;
    private AtomicRef<Node> tail;

    public MSQueue() {
        Node dummy = new Node(0, null);
        this.head = new AtomicRef<>(dummy);
        this.tail = new AtomicRef<>(dummy);
    }

    @Override
    public void enqueue(int x) {
        Node newTail = new Node(x, null);
        while (true) {
            goMyTail();
            Node val = tail.getValue();
            if (val.next.compareAndSet(null, newTail)) {
                tail.compareAndSet(val, newTail);
                return;
            }
        }
    }

    private void goMyTail() {
        Node val = tail.getValue();
        Node nextVal = val.next.getValue();
        while (nextVal != null) {
            tail.compareAndSet(val, val.next.getValue());
            val = tail.getValue();
            nextVal = val.next.getValue();
        }
    }

    @Override
    public int dequeue() {
        while (true) {
            Node curHead = head.getValue();
            goMyTail();
            Node curTail = tail.getValue();
            if (tail.compareAndSet(curTail, curTail)) {
                if (head.compareAndSet(curHead, curHead)) {
                    if (curHead == curTail) {
                        return Integer.MIN_VALUE;
                    }
                } else {
                    continue;
                }
            } else {
                continue;
            }
            Node nextHead = curHead.next.getValue();
            if (head.compareAndSet(curHead, nextHead)) {
                if (nextHead == null) {
                    return Integer.MIN_VALUE;
                }
                return nextHead.x;
            }
        }
    }

    @Override
    public int peek() {
        while (true) {
            Node curHead = head.getValue();
            Node nextHead = curHead.next.getValue();
            goMyTail();
            Node curTail = tail.getValue();
            if (tail.compareAndSet(curTail, curTail)) {
                if (head.compareAndSet(curHead, curHead)) {
                    if (curHead == curTail) {
                        return Integer.MIN_VALUE;
                    }
                } else {
                    continue;
                }
            } else {
                continue;
            }
            if (head.compareAndSet(curHead, curHead)) {
                if (nextHead == null) {
                    return Integer.MIN_VALUE;
                }
                return nextHead.x;
            }
        }
    }
}