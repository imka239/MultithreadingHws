package stack;

public class Main {
    public static void main(String[] args) {
        Queue q = new MSQueue();
        q.enqueue(10);
        q.enqueue(20);
        System.out.println(q.peek());
        System.out.println(q.dequeue());
        System.out.println(q.dequeue());
        System.out.println(q.dequeue());
        q.enqueue(10);
        q.enqueue(20);
        System.out.println(q.dequeue());
        System.out.println(q.dequeue());
    }
}
