package webdata;

/**
 * Pair class.
 *
 * @param <L> left value
 * @param <R> right value
 */
public class Pair<L, R> {
    private L l;
    private R r;

    /**
     * Pair constructor.
     *
     * @param l left value
     * @param r right value
     */
    public Pair(L l, R r) {
        this.l = l;
        this.r = r;
    }

    /**
     * @return left element in the pair.
     */
    public L getL() {
        return l;
    }

    /**
     * @return right element in the pair.
     */
    public R getR() {
        return r;
    }

    /**
     * Sets left element in the pair.
     *
     * @param l new left value.
     */
    public void setL(L l) {
        this.l = l;
    }

    /**
     * Sets right element in the pair.
     *
     * @param r new left value.
     */
    public void setR(R r) {
        this.r = r;
    }
}