package linked_list_set;

import kotlinx.atomicfu.*;

public class SetImpl implements Set {

    private static class AbstractNode {
        int x;

        static Node asReal(AbstractNode node) {
            return node instanceof Node ? (Node) node : null;
        }

        static RemovedNode asRemoved(AbstractNode node) {
            return node instanceof RemovedNode ? (RemovedNode) node : null;
        }

        static AbstractNode next(AbstractNode node) {
            return node instanceof Node ? asReal(node).next.getValue() : next(asRemoved(node).node);
        }

        AbstractNode(int x) {
            this.x = x;
        }
    }

    private class Node extends AbstractNode {
        AtomicRef<AbstractNode> next;

        Node(int x, AbstractNode next) {
            super(x);
            this.next = new AtomicRef<>(next);
        }
    }

    private class RemovedNode extends AbstractNode {
        Node node;

        RemovedNode(Node node) {
            super(node.x);
            this.node = node;
        }
    }

    private static class Window {
        Node cur, next;

        Window(Node cur, Node next) {
            this.cur = cur;
            this.next = next;
        }
    }

    private final AtomicRef<? extends AbstractNode> head = new AtomicRef<>(new Node(Integer.MIN_VALUE, new Node(Integer.MAX_VALUE, null)));

    private Window goFrom(Node cur, Node next, int x) {
        AbstractNode afterNext = AbstractNode.next(next);
        if (afterNext instanceof RemovedNode) {
            Node afterNextRem = AbstractNode.asRemoved(afterNext).node;
            return cur.next.compareAndSet(next, afterNextRem) ? goFrom(cur, afterNextRem, x) : null;
        } else {
            return next.x < x ? goFrom(next, AbstractNode.asReal(afterNext), x) : new Window(cur, next);
        }
    }

    /**
     * Returns the {@link Window}, where cur.x < x <= next.x
     */
    private Window findWindow(int x) {
        Window ans;
        do {
            Node cur = AbstractNode.asReal(head.getValue());
            Node next = AbstractNode.asReal(AbstractNode.next(cur));
            ans = goFrom(cur, next, x);
        } while (ans == null);
        return ans;
    }

    @Override
    public boolean add(int x) {
        while (true) {
            Window w = findWindow(x);
            if (w.next.x == x) {
                return false;
            }
            AbstractNode node = new Node(x, w.next);
            if (w.cur.next.compareAndSet(w.next, node)) {
                return true;
            }
        }
    }

    @Override
    public boolean remove(int x) {
        while (true) {
            Window w = findWindow(x);
            if (w.next.x != x) {
                return false;
            }
            AbstractNode nodeAfterX = w.next.next.getValue();
            if (nodeAfterX instanceof RemovedNode) {
                continue;
            }
            RemovedNode newNode = new RemovedNode(AbstractNode.asReal(nodeAfterX));
            if (w.next.next.compareAndSet(nodeAfterX, newNode)) {
                w.cur.next.compareAndSet(w.next, nodeAfterX);
                return true;
            }
        }
    }

    @Override
    public boolean contains(int x) {
        Window w = findWindow(x);
        return w.next.x == x;
    }
}