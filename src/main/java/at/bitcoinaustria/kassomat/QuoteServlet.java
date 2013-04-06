package at.bitcoinaustria.kassomat;

import at.bitcoinaustria.Bitcoins;
import at.bitcoinaustria.ECKeyFactory;
import com.google.bitcoin.core.*;
import com.google.bitcoin.store.BlockStoreException;
import com.google.bitcoin.store.BoundedOverheadBlockStore;
import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocketServlet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

/**
 * @author apetersson
 */
public class QuoteServlet extends WebSocketServlet {

    public static final Environment ENV = Environment.getEnv();
    public static final int TIMEOUT_MILLIS = 1000 * 60 * 15;
    private final static YamlHelper yamlHelper = new YamlHelper();
    private PeerGroup peerGroup;
    private Wallet wallet;
    private ECKeyFactory ecKeyFactory;


    private static double getEuroPerBitcoin() {
        return 108.99;
    }

    @Override
    public void init() throws ServletException {
        wallet = loadWallet();
        ecKeyFactory = new ECKeyFactory(wallet, ENV.getSeed());
        if (wallet.keychain.isEmpty()){
            ecKeyFactory.newKey();
        }
        File blockFile = new File("blockstore-" + ENV.name() + ".dat");
        try {
            BoundedOverheadBlockStore bs = new BoundedOverheadBlockStore(wallet.getParams(), blockFile);
            BlockChain bc = new BlockChain(ENV.getParameters(), wallet, bs);
            peerGroup = new PeerGroup(ENV.getParameters(), bc);
            ENV.addDiscovery(peerGroup);
            peerGroup.addWallet(wallet);
            peerGroup.start();
            peerGroup.downloadBlockChain();
        } catch (BlockStoreException e) {
            throw new RuntimeException(e);
        }
        Bitcoins avail = Bitcoins.valueOf(wallet.getBalance(Wallet.BalanceType.AVAILABLE).longValue());
        Bitcoins est = Bitcoins.valueOf(wallet.getBalance(Wallet.BalanceType.ESTIMATED).longValue());
        String different = avail.equals(est) ? "" : "/ " + est.toCurrencyString();
        System.out.println("finished init, wallet contains: " + avail.toCurrencyString() + different);
        super.init();
    }

    @Override
    public void destroy() {
        peerGroup.stop();
    }

    private Wallet loadWallet() {
        try {
            File wF = new File("wallet-" + ENV.name() + ".dat");
            Wallet wallet;
            if (wF.exists()) {
                wallet = Wallet.loadFromFile(wF);
            } else {
                wallet = new Wallet(ENV.getParameters());
            }
            wallet.autosaveToFile(wF, 50, TimeUnit.MILLISECONDS, null);
            return wallet;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public WebSocket doWebSocketConnect(HttpServletRequest request, String protocol) {
        ENV.checkSSL(request);
        return new QuoteSocket();
    }

    private String btcFormat(BigInteger available) {
        return Bitcoins.valueOf(available.longValue()).toCurrencyString();
    }

    private class QuoteSocket implements WebSocket, WebSocket.OnTextMessage {
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

            if (data.startsWith("sendbtc:\n")) {
                data = data.replaceFirst("sendbtc:\\n", "");
                handleSendBtc(data);
            }


        }

        private void handleSendBtc(String data) {
            SendBtcRequest req = yamlHelper.readSendRequest(data);
            Bitcoins toSend = Bitcoins.nearestValue(req.eurocent / 100.0 / getEuroPerBitcoin());
            try {
                try {
                    BigInteger toSendBI = toSend.toBigInteger();
                    BigInteger available = wallet.getBalance(Wallet.BalanceType.AVAILABLE);
                    if (available.compareTo(toSendBI) < 0) {
                        connection.sendMessage("sendbtc: \nerror: \"not enough funds. requested:" + btcFormat(toSendBI) + "avail:" + btcFormat(available) + "\"");
                    } else {
                        Wallet.SendResult sendResult = wallet.sendCoins(peerGroup, new Address(ENV.getParameters(), req.address), toSendBI);
                        if (sendResult == null) { //should only trigger when highly concurrent...
                            connection.sendMessage("sendbtc: \nerror: \"not enough funds\"");
                        } else {
                            connection.sendMessage(new SentBtcReply(sendResult.tx.getHashAsString()).toYaml());
                        }
                    }
                } catch (AddressFormatException e) {
                    connection.sendMessage("sendbtc: \nerror: \"wrong address format:'" + req.address + "'\"");
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private void handleQuote(String data) {
            final QuoteRequest quoteRequest;
            final QuoteReply uri;
            quoteRequest = yamlHelper.readQuoteRequest(data);
            final Bitcoins btcamout = Bitcoins.nearestValue(quoteRequest.getEurocent() / getEuroPerBitcoin() / 100);
            final ECKey ecKey = ecKeyFactory.newKey();
            final Address destAddr = ecKey.toAddress(ENV.getParameters());
            wallet.addKey(ecKey);
            uri = sendQuoteReply(btcamout, ecKey);
            final Timer t = new Timer();
            final AbstractWalletEventListener listener = new AbstractWalletEventListener() {
                @Override
                public void onCoinsReceived(Wallet wallet, Transaction tx, BigInteger prevBalance, BigInteger newBalance) {
                    for (TransactionOutput output : tx.getOutputs()) {
                        try {
                            Script script = output.getScriptPubKey();
                            if (script.isSentToAddress() && script.getToAddress().equals(destAddr)) {
                                System.out.println("recieved money!");
                                BigInteger overpaid = output.getValue().subtract(btcamout.toBigInteger());
                                if (overpaid.compareTo(BigInteger.ZERO) == 0) {
                                    sendPaymentRecieved(uri, tx.getHash().toString(), quoteRequest.getEurocent());
                                } else {
                                    BigDecimal euroOverpaid = Bitcoins.valueOf(overpaid.longValue()).multiply(BigDecimal.valueOf(getEuroPerBitcoin()));
                                    long centOverpaid = (long) (euroOverpaid.doubleValue() * 100);
                                    sendPaymentRecieved(uri, tx.getHash().toString(), quoteRequest.getEurocent() + centOverpaid);
                                }
                            }
                        } catch (ScriptException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    t.cancel();
                    wallet.removeEventListener(this);
                }
            };

            wallet.addEventListener(listener);
            t.schedule(new TimerTask() {
                @Override
                public void run() {
                    wallet.removeEventListener(listener);
                }
            }, TIMEOUT_MILLIS);


        }

        private void sendPaymentRecieved(QuoteReply uri, String txid, long eurocent) {
            QuotePaidReply ok = new QuotePaidReply(uri, eurocent, txid);
            try {
                connection.sendMessage(ok.toYaml());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private QuoteReply sendQuoteReply(Bitcoins btcamout, ECKey ecKey) {
            try {
                NetworkParameters params = ENV.getParameters();
                Address addr = ecKey.toAddress(params);
                QuoteReply reply = new QuoteReply(addr, btcamout);
                connection.sendMessage(reply.toYaml());
                return reply;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
