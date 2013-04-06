package at.bitcoinaustria.kassomat;

/**
 * @author apetersson
 */
public class QuoteRequest {

    long eurocent;

    public long getEurocent() {
        return eurocent;
    }

    public void setEurocent(long eurocent) {
        this.eurocent = eurocent;
    }

    @Override
    public String toString() {
        return "QuoteRequest{" +
                "eurocent=" + eurocent +
                '}';
    }
}
