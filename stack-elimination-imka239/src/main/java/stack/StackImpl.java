package stack;

import kotlinx.atomicfu.AtomicRef;

import java.util.Random;


public class StackImpl implements Stack {
    private static class Node {
        final AtomicRef<Node> next;
        final int x;

        Node(int x, Node next) {
            this.next = new AtomicRef<>(next);
            this.x = x;
        }
    }

    final int SIZE = 10;

    StackImpl() {
        for (int i = 0; i < SIZE; i++) {
            tasks[i] = new AtomicRef<>(null);
        }
    }

    // head pointer
    private AtomicRef<Node>[] tasks = new AtomicRef[SIZE];
    private AtomicRef<Node> head = new AtomicRef<>(null);

    @Override
    public void push(int x) {
        Random rnd = new Random();
        int pos = ((rnd.nextInt() % SIZE) + SIZE) % SIZE;
        for (int i = 0; i < 2; i++) {
            Node node = new Node(x, null);
            if (tasks[(pos + i) % SIZE].getValue() == null) {
                if (tasks[(pos + i) % SIZE].compareAndSet(null, node)) {
                    int y = 0;
                    for (int j = 0; j < 10; j++) {
                        y++;
                    }
                    if (tasks[(pos + i) % SIZE].compareAndSet(node, null)) {
                        break;
                    } else {
                        return;
                    }
                }
            }
            if (tasks[(pos - i + SIZE) % SIZE].getValue() == null) {
                if (tasks[(pos - i) % SIZE].compareAndSet(null, node)) {
                    int y = 0;
                    for (int j = 0; j < 10; j++) {
                        y++;
                    }
                    if (tasks[(pos - i) % SIZE].compareAndSet(node, null)) {
                        break;
                    } else {
                        return;
                    }
                }
            }
        }
        while (true) {
            Node val = head.getValue();
            Node newHead = new Node(x, val);
            if (head.compareAndSet(val, newHead)) {
                return;
            }
        }
    }

    @Override
    public int pop() {
        Random rnd = new Random();
        int pos = ((rnd.nextInt() % SIZE) + SIZE) % SIZE;
        for (int j = 0; j < 10; j++) {
            for (int i = 0; i < 2; i++) {
                if (tasks[(pos + i) % SIZE].getValue() != null) {
                    Node curNode = tasks[(pos + i) % SIZE].getValue();
                    if (tasks[(pos + i) % SIZE].compareAndSet(curNode, null)) {
                        if (curNode != null) {
                            return curNode.x;
                        } else {
                            continue;
                        }
                    }
                }
                if (tasks[(pos - i + SIZE) % SIZE].getValue() != null) {
                    Node curNode = tasks[(pos - i + SIZE) % SIZE].getValue();
                    if (tasks[(pos - i + SIZE) % SIZE].compareAndSet(curNode, null)) {
                        if (curNode != null) {
                            return curNode.x;
                        } else {
                            continue;
                        }
                    }
                }
            }
        }
        while (true) {
            Node curHead = head.getValue();
            if (curHead == null) return Integer.MIN_VALUE;
            if (head.compareAndSet(curHead, curHead.next.getValue())) {
                return curHead.x;
            }
        }
    }
}
