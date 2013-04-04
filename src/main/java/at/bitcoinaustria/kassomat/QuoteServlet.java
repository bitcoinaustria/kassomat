package at.bitcoinaustria.kassomat;

import com.esotericsoftware.yamlbeans.YamlConfig;
import com.esotericsoftware.yamlbeans.YamlException;
import com.esotericsoftware.yamlbeans.YamlReader;
import com.esotericsoftware.yamlbeans.YamlWriter;
import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocketServlet;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.StringWriter;

/**
 * @author apetersson
 */
public class QuoteServlet extends WebSocketServlet {

    static {
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                e.printStackTrace();
            }
        });
    }

    private static double getEuroPerBitcoin() {
        return 140.0;
    }

    @Override
    public WebSocket doWebSocketConnect(HttpServletRequest request, String protocol) {
        return new QuoteSocket();
    }

    private static class QuoteSocket implements WebSocket, WebSocket.OnTextMessage {
        private Connection connection;

        @Override
        public void onOpen(Connection connection) {
            this.connection = connection;
        }

        @Override
        public void onClose(int closeCode, String message) {
        }

        @Override
        public void onMessage(String data) {
            if (data.startsWith("quote:\n")) {
                data = data.replaceFirst("quote:\\n", "");
                handleQuote(data);
            }

        }

        private void handleQuote(String data) {
            YamlConfig config = new YamlConfig();
            config.setClassTag("quote", Quote.class);

            final Quote quote;

            final String uri;
            try {
                quote = new YamlReader(data, config).read(Quote.class);
                double btcamout = quote.getEurocent() / getEuroPerBitcoin() / 100;
                uri = sendMessage(btcamout);
            } catch (YamlException e) {
                throw new RuntimeException(e);
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            String txid = "1234567890";
            sendOk(uri, txid, quote.getEurocent());


        }

        private void sendOk(String uri, String txid, long eurocent) {
            Sendok ok = new Sendok(uri, eurocent, txid);
            StringWriter w = new StringWriter();
            YamlConfig config = new YamlConfig();
            config.setClassTag("recievebtc", Sendok.class);
            try {
                new YamlWriter(w, config).write(ok);
            } catch (YamlException e) {
                throw new RuntimeException(e);
            }
            try {
                connection.sendMessage(w.toString());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private String sendMessage(double btcamout) {
            try {
                String uri = String.format("quote:\n uri: \"bitcoin:1GsFNGcThqVQNwfRjMrxwnCuc64toqY7ax?amount=%.8f\"", btcamout);
                connection.sendMessage(uri);
                return uri;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
