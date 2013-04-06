package at.bitcoinaustria.kassomat;

import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.PeerAddress;
import com.google.bitcoin.core.PeerGroup;
import com.google.bitcoin.discovery.DnsDiscovery;
import com.google.bitcoin.discovery.IrcDiscovery;
import com.google.common.base.Preconditions;

import javax.security.cert.X509Certificate;
import javax.servlet.http.HttpServletRequest;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

/**
 * @author apetersson
 */
public enum Environment {
    TEST(NetworkParameters.testNet()) {
        @Override
        public String getSeed() {
            return "5/4/13 North Korea warns embassies over safety following missile threat";
        }

        @Override
        public void addDiscovery(PeerGroup peerGroup) {
            try {
                peerGroup.addAddress(new PeerAddress(InetAddress.getLocalHost(), params.port));
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
            peerGroup.addPeerDiscovery(new IrcDiscovery("#bitcoinTEST3"));
        }

        @Override
        public void checkSSL(HttpServletRequest request) {
            //do nothing
        }
    }, PROD(NetworkParameters.prodNet()) {
        @Override
        public String getSeed() {
            String property = System.getProperty("kassomat.seed");
            if (property == null) {
                throw new IllegalArgumentException("please provide a secure, secret string seed for the wallet: -Dkassomat.seed=123456");
            }
            return property;
        }

        @Override
        public void addDiscovery(PeerGroup peerGroup) {
            peerGroup.addPeerDiscovery(new DnsDiscovery(params));
        }

        @Override
        public void checkSSL(HttpServletRequest request) {
            X509Certificate cert = extractCertificate(request);
            byte[] fingerprint = cert.getSerialNumber().toByteArray();
            boolean matched = Arrays.equals(fingerprint, this.fingerprint);
            Preconditions.checkArgument(matched, "given fingerprint %s did not match expected %s"
                    , fingerprint, this.fingerprint);
        }
    };
    protected final NetworkParameters params;
    protected byte[] fingerprint;

    private Environment(NetworkParameters params) {
        this.params = params;
    }

    public static Environment getEnv() {
        String envStr = System.getProperty("kassomat.env");
        if (envStr == null) {
            throw new IllegalArgumentException("please specify environment with -Dkassomat.env=test or -Dkassomat.env=prod");
        }
        if (envStr.toLowerCase().equals("prod")) {
            parseFingerprint();
            return PROD;
        }
        return TEST;
    }

    private static void parseFingerprint() {
        String fingerprint = System.getProperty("kassomat.fingerprint");
        if (fingerprint == null) {
            throw new IllegalArgumentException("missing -Dkassomat.fingerprint system property in hex");
        }
        PROD.fingerprint = new BigInteger(fingerprint, 16).toByteArray();
        //todo assert size
    }

    public NetworkParameters getParameters() {
        return params;
    }

    public abstract void checkSSL(HttpServletRequest request);

    X509Certificate extractCertificate(HttpServletRequest request) {
        X509Certificate[] certs = (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate");
        if (null != certs && certs.length > 0) {
            return certs[0];
        }
        throw new RuntimeException("No X.509 client certificate found in request");
    }

    public abstract void addDiscovery(PeerGroup peerGroup);

    public abstract String getSeed();
}
