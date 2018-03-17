package tech.furszy.multisig;

import org.pivxj.core.NetworkParameters;
import org.pivxj.core.Sha256Hash;
import org.pivxj.core.TransactionOutPoint;
import org.pivxj.script.Script;

public class OutputWrapper {

    private long index;
    private byte[] scriptBytes;
    private Sha256Hash txId;
    private Script redeemScript;

    public OutputWrapper(long index, byte[] scriptBytes, Sha256Hash txId) {
        this.index = index;
        this.scriptBytes = scriptBytes;
        this.txId = txId;
    }

    public OutputWrapper(long index, byte[] scriptBytes, Sha256Hash txId, Script redeemScript) {
        this.index = index;
        this.scriptBytes = scriptBytes;
        this.txId = txId;
        this.redeemScript = redeemScript;
    }

    public long getIndex() {
        return index;
    }

    public byte[] getScriptBytes() {
        return scriptBytes;
    }

    public Sha256Hash getTxId() {
        return txId;
    }

    public Script getRedeemScript() {
        return redeemScript;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        OutputWrapper that = (OutputWrapper) o;

        if (index != that.index) return false;
        return txId.equals(that.txId);
    }

    @Override
    public int hashCode() {
        int result = (int) (index ^ (index >>> 32));
        result = 31 * result + txId.hashCode();
        return result;
    }
}
