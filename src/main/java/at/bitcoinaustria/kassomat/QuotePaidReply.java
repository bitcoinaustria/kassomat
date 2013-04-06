package at.bitcoinaustria.kassomat;

/**
 * @author apetersson
 */
public class QuotePaidReply {
    QuoteReply origReply;
    long eurocent;
    String txid;

    public QuotePaidReply(QuoteReply origReply, long eurocent, String txid) {
        this.origReply = origReply;
        this.eurocent = eurocent;
        this.txid = txid;
    }

    public String toYaml() {
        return "recievebtc:"
                + "\nuri: " + origReply.uri
                + "\neurocent: " + eurocent
                + "\ntxid: " + txid;
    }
}
