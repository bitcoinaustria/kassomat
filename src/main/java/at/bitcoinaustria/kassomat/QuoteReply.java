package at.bitcoinaustria.kassomat;

import at.bitcoinaustria.Bitcoins;
import com.google.bitcoin.core.Address;

/**
 * @author apetersson
 */
public class QuoteReply {

    public String uri;

    public QuoteReply(Address address, Bitcoins amount) {
        this.uri = getUri(address, amount);
    }

    private String getUri(Address address, Bitcoins amount) {
        return "bitcoin:" + address.toString() + "?amount=" + amount.toString();
    }

    public String toYaml() {
        return "quote:\n" +
                "uri: " + uri;
    }
}
