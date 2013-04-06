package at.bitcoinaustria;

import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.Wallet;
import com.google.common.base.Charsets;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * a programmer
 */
public class ECKeyFactory {
    private final Wallet saveTo;
    private final String seed;
    private AtomicLong counter = new AtomicLong(0);

    public ECKeyFactory(Wallet saveTo, String seed) {
        this.saveTo = saveTo;
        this.seed = seed;
        boolean hasKey = true;
        while (hasKey) {
            ECKey newKey = buildKey();
            if (!saveTo.isPubKeyMine(newKey.getPubKey())) {
                hasKey = false;
                counter.decrementAndGet();
            }
        }
        System.out.println("continuing detkey at index " + counter);
    }

    public ECKey newKey() {
        ECKey ret = buildKey();
        saveTo.addKey(ret);
        return ret;
    }

    private ECKey buildKey() {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        String toHash = seed + counter.incrementAndGet();
        md.update(toHash.getBytes(Charsets.UTF_8));
        return new ECKey(new BigInteger(md.digest()).abs());
    }

}
