package tech.furszy.multisig;

import com.google.common.collect.ImmutableList;
import org.pivxj.core.*;
import org.pivxj.crypto.ChildNumber;
import org.pivxj.crypto.DeterministicKey;
import org.pivxj.crypto.TransactionSignature;
import org.pivxj.script.Script;
import org.pivxj.signers.StatelessTransactionSigner;
import org.pivxj.wallet.BasicKeyChain;
import org.pivxj.wallet.KeyBag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>This signer may be used as a template for creating custom multisig transaction signers.</p>
 * <p>
 * Concrete implementations have to implement {@link #getSignature(org.pivxj.core.Sha256Hash, java.util.List)}
 * method returning a signature and a public key of the keypair used to created that signature.
 * It's up to custom implementation where to locate signatures: it may be a network connection,
 * some local API or something else.
 * </p>
 */
public class MultiSigTransactionSigner extends StatelessTransactionSigner {
    private static final Logger log = LoggerFactory.getLogger(MultiSigTransactionSigner.class);

    private BasicKeyChain keyChain;
    private Script redeemScript;

    public MultiSigTransactionSigner(BasicKeyChain keyChain, Script redeemScript) {
        this.keyChain = keyChain;
        this.redeemScript = redeemScript;
    }

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public boolean signInputs(ProposedTransaction propTx, KeyBag keyBag) {
        Transaction tx = propTx.partialTx;
        int numInputs = tx.getInputs().size();
        for (int i = 0; i < numInputs; i++) {
            TransactionInput txIn = tx.getInput(i);
            TransactionOutput txOut = txIn.getConnectedOutput();
            if (txOut == null) {
                continue;
            }
            Script scriptPubKey = txOut.getScriptPubKey();
            if (!scriptPubKey.isPayToScriptHash()) {
                log.warn("MultiSigTransactionSigner works only with P2SH transactions");
                return false;
            }

            Script inputScript = checkNotNull(txIn.getScriptSig());

            try {
                // We assume if its already signed, its hopefully got a SIGHASH type that will not invalidate when
                // we sign missing pieces (to check this would require either assuming any signatures are signing
                // standard output types or a way to get processed signatures out of script execution)
                txIn.getScriptSig().correctlySpends(tx, i, txIn.getConnectedOutput().getScriptPubKey());
                log.warn("Input {} already correctly spends output, assuming SIGHASH type used will be safe and skipping signing.", i);
                continue;
            } catch (ScriptException e) {
                // Expected.
            }

            Sha256Hash sighash = tx.hashForSignature(i, redeemScript, Transaction.SigHash.ALL, false);
            SignatureAndKey sigKey = getSignature(sighash, propTx.keyPaths.get(scriptPubKey));
            TransactionSignature txSig = new TransactionSignature(sigKey.sig, Transaction.SigHash.ALL, false);
            int sigIndex = inputScript.getSigInsertionIndex(sighash, sigKey.pubKey);
            inputScript = scriptPubKey.getScriptSigWithSignature(inputScript, txSig.encodeToBitcoin(), sigIndex);
            txIn.setScriptSig(inputScript);
        }
        return true;
    }


    protected SignatureAndKey getSignature(Sha256Hash sighash, List<ChildNumber> derivationPath) {
        ImmutableList<ChildNumber> keyPath = ImmutableList.copyOf(derivationPath);
        DeterministicKey key = null;// keyChain.getKeyByPath(keyPath, true);
        return new SignatureAndKey(key.sign(sighash), key.dropPrivateBytes().dropParent());
    }

    public class SignatureAndKey {
        public final ECKey.ECDSASignature sig;
        public final ECKey pubKey;

        public SignatureAndKey(ECKey.ECDSASignature sig, ECKey pubKey) {
            this.sig = sig;
            this.pubKey = pubKey;
        }
    }

}
