package at.bitcoinaustria;

/**
 * @author apetersson
 */
public class Quote {
    long eurocent;

    public long getEurocent() {
        return eurocent;
    }

    public void setEurocent(long eurocent) {
        this.eurocent = eurocent;
    }

    @Override
    public String toString() {
        return "Quote{" +
                "eurocent=" + eurocent +
                '}';
    }
}
