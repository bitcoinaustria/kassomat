package at.bitcoinaustria.kassomat;

/**
 * @author apetersson
 */
public class Sendok {
    String uri;
    long eurocent;
    String txid;

    public Sendok(String uri, long eurocent, String txid) {
        this.uri = uri;
        this.eurocent = eurocent;
        this.txid = txid;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public long getEurocent() {
        return eurocent;
    }

    public void setEurocent(long eurocent) {
        this.eurocent = eurocent;
    }

    public String getTxid() {
        return txid;
    }

    public void setTxid(String txid) {
        this.txid = txid;
    }
}
