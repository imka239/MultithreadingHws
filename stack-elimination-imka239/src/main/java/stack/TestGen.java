import java.io.*;
import java.util.*;

public class TestsGen implements Runnable {
    public static void main(String[] args) {
        if (args.length < 1) {
            for (I = 1; (new File(getName(I) + ".t")).exists(); I++)
                ;
        } else {
            I = Integer.parseInt(args[0]);
        }
        new Thread(new TestsGen()).start();
    }

    PrintWriter out;

    Random rand = new Random(6439586L);

    static int I;

    static String getName(int i) {
        return ((i < 10) ? "0" : "") + i;
    }

    void open() {
        try {
            System.out.println("Generating test " + I);
            out = new PrintWriter(getName(I));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    void close() {
        out.close();
        I++;
    }

    final String ALPHA = "abcdefghijklmnopqrstuvwxyz";

    String randString(int len, String alpha) {
        StringBuilder ans = new StringBuilder();
        int k = alpha.length();
        for (int i = 0; i < len; i++) {
            ans.append(alpha.charAt(rand.nextInt(k)));
        }
        return ans.toString();
    }

    long rand(long l, long r) {
        return l + (rand.nextLong() >>> 1) % (r - l + 1);
    }

    int rand(int l, int r) {
        return l + rand.nextInt(r - l + 1);
    }

    class Pair {
        int a, b;

        public Pair(int a, int b) {
            this.a = a;
            this.b = b;
        }

        @Override
        public String toString() {
            return a + " " + b;
        }
    }

    class CNM {
        int[] p, r;

        public CNM(int n) {
            p = new int[n];
            r = new int[n];
            for (int i = 0; i < n; ++i) {
                p[i] = i;
            }
        }

        int get(int i) {
            if (p[i] != i) p[i] = get(p[i]);
            return p[i];
        }

        void union(int i, int j) {
            i = get(i);
            j = get(j);
            if (i == j) return;
            if (r[i] == r[j]) ++r[i];
            if (r[i] > r[j]) {
                p[j] = i;
            } else {
                p[i] = j;
            }
        }
    }

    int dbg_cnt = 0;

    ArrayList<Pair> genComponent(int l, int r, int m, boolean bp) {
        if (m < r - l) throw new Error("Not enough!!");
        ArrayList<Pair> ans = new ArrayList<Pair>();
        int[] col = new int[r - l + 1];
        for (int i = l + 1; i <= r; ++i) {
            int q = rand(l, i - 1);
            col[i - l] = 1 - col[q - l];
            ans.add(new Pair(q, i));
        }
        ArrayList<Integer> L = new ArrayList<Integer>();
        ArrayList<Integer> R = new ArrayList<Integer>();
        for (int i = l; i <= r; ++i) {
            if (col[i - l] == 0) {
                L.add(i);
            } else {
                R.add(i);
            }
        }
        for (int i = r - l; i < m; ++i) {
            int a = L.get(rand.nextInt(L.size()));
            int b = R.get(rand.nextInt(R.size()));
            ans.add(new Pair(a, b));
        }
        if (!bp) {
            int a = L.get(rand.nextInt(L.size()));
            int b = L.get(rand.nextInt(L.size()));
            ans.get(ans.size() - 1).a = a;
            ans.get(ans.size() - 1).b = b;
        }
        if (ans.size() != m) throw new Error();
        return ans;
    }

    ArrayList<Integer> getSplitting(int n, int k) {
        ArrayList<Integer> sz = new ArrayList<Integer>();
        if (n == 0 && k == 0) return sz;
        int tn = n;
        int f = n / k * 2;
        for (int i = 0; i < k - 1; ++i) {
            int ts = rand(0, Math.min(tn, f));
            sz.add(ts);
            tn -= ts;
        }
        sz.add(tn);
        return sz;
    }

    int[] genPerm(int n) {
        int[] ans = new int[n];
        for (int i = 0;i < n; ++i) {
            ans[i] = i;
        }
        for (int i = 1; i < n; ++i) {
            int j = rand(0, i - 1);
            int t = ans[i];
            ans[i] = ans[j];
            ans[j] = t;
        }
        return ans;
    }

    void genRandTest(int n, int m, int k, boolean bp) {
        if (n < k) {
            throw new Error();
        }
        ArrayList<Integer> vs = getSplitting(n - k, k);
        ArrayList<Integer> eli = new ArrayList<Integer>();
        for (int i = 0; i < vs.size(); ++i) {
            if (vs.get(i) > 0) {
                eli.add(i);
            }
            vs.set(i, vs.get(i) + 1);
        }
        ArrayList<Integer> tes = getSplitting(m - n + k, eli.size());
        ArrayList<Integer> es = new ArrayList<Integer>();
        int j = 0;
        for (int i = 0; i < k; ++i) {
            if (vs.get(i) > 1) {
                es.add(tes.get(j++));
            } else {
                es.add(0);
            }
        }
        ArrayList<Integer> cand = new ArrayList<Integer>();
        for (int i = 0; i < vs.size(); ++i) {
            if (es.get(i) > 0) {
                cand.add(i);
            }
            es.set(i, es.get(i) + vs.get(i) - 1);
        }
        ArrayList<Pair> E = new ArrayList<Pair>();
        int l = 0;
        int lucky = bp ? -1 : cand.get(rand.nextInt(cand.size()));
        for (int i = 0; i < k; ++i) {
            E.addAll(genComponent(l, l + vs.get(i) - 1, es.get(i), lucky != i));
            l += vs.get(i);
        }

        int[] perm = genPerm(n);

        for (Pair p : E) {
            p.a = perm[p.a];
            p.b = perm[p.b];
        }

        open();
        out.println(n + " " + m);
        for (int i = 0; i < m; ++i) {
            Pair p = E.get(i);
            out.println((p.a) + " " + (p.b));
        }
        int x = rand(1, n / 2);
        out.println(x);
        for (int i = 0; i < x; i++) {
            out.println(rand(1, n) + " " + rand(1, 2));
        }
        close();
    }


    final int MAXN = 100000;
    final int MAXM = 200000;

    public void solve() throws IOException {
        for (int i = 0; i < 10; ++i) {
            int n = rand(2, 10);
            int k = rand(1, n - 1);
            int m = rand(n - k + 1, 20);
            genRandTest(n, m, k, rand.nextBoolean());
        }
        for (int i = 0; i < 20; ++i) {
            int n = rand(MAXN / 2, MAXN);
            int k = rand(1, n - 1);
            int m = rand(n - k + 1, MAXM);
            genRandTest(n, m, k, rand.nextBoolean());
        }
        genRandTest(MAXN, MAXM, 1, true);
        genRandTest(MAXN, MAXM, 1, false);
        genRandTest(MAXN, MAXN - 1, 1, true);
        genRandTest(MAXN, MAXN, 1, false);
        genRandTest(MAXN, MAXN - 9, 10, false);
        genRandTest(MAXN, MAXN - 10, 10, true);
        genRandTest(MAXN, MAXM, 10, true);
        genRandTest(MAXN, 0, MAXN, true);
    }

    void myAssert(boolean e, String msg) {
        if (!e) {
            throw new Error(msg);
        }
    }

    public void run() {
        try {
            solve();
        } catch (Throwable e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }
}