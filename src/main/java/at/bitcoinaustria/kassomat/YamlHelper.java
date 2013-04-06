package at.bitcoinaustria.kassomat;

import com.esotericsoftware.yamlbeans.YamlConfig;
import com.esotericsoftware.yamlbeans.YamlException;
import com.esotericsoftware.yamlbeans.YamlReader;

/**
 * @author apetersson
 */
public class YamlHelper {


    private final YamlConfig config;

    public YamlHelper() {
        config = new YamlConfig();
        config.setClassTag("quote", QuoteRequest.class);
        config.setClassTag("quote", QuoteReply.class);
        config.setClassTag("recievebtc", QuotePaidReply.class);

    }

    private YamlReader getReader(String data) {
        return new YamlReader(data, config);
    }

    public QuoteRequest readQuoteRequest(String data) {
        try {
            return getReader(data).read(QuoteRequest.class);
        } catch (YamlException e) {
            throw new RuntimeException(e);
        }
    }

    public SendBtcRequest readSendRequest(String data) {
        try {
            return getReader(data).read(SendBtcRequest.class);
        } catch (YamlException e) {
            throw new RuntimeException(e);
        }
    }
}
