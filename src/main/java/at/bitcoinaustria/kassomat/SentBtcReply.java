package at.bitcoinaustria.kassomat;

/**
 * @author apetersson
 */
public class SentBtcReply {
    public final String txid;

    public SentBtcReply(String txid) {
        this.txid = txid;
    }

    public String toYaml() {
        return "sentbtc: " +
                "\ntxid: " + txid;
    }
}
