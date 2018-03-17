package tech.furszy.multisig;

import com.google.common.collect.Lists;
import com.sun.istack.internal.Nullable;
import org.pivxj.core.*;
import org.pivxj.script.Script;
import org.pivxj.script.ScriptBuilder;
import org.pivxj.signers.TransactionSigner;
import org.pivxj.wallet.*;
import org.spongycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MultiSigBuilder {

    private NetworkParameters params;
    private TransactionSigner transactionSigner;

    public MultiSigBuilder(NetworkParameters params) {
        this.params = params;
    }

    /**
     * Create a multiSig address from M amount of pubKeys and a N amount of required signatures.
     * @param threshold --> minimum amount of required signatures.
     * @param pubKeys
     * @return
     */
    public MultiSigAddress createMultiSigAddress(int threshold,String[] pubKeys) {
        List<ECKey> keys = new ArrayList<>();

        for (String pubKey : pubKeys) {
            keys.add(
                    ECKey.fromPublicOnly(Hex.decode(pubKey))
            );
        }

        // Create a multiSig output script.
        Script redeemScript = ScriptBuilder.createMultiSigOutputScript(threshold, keys);
        // Create the pay to script hash script using RIPEMD160(sha256(redeemScript)).
        Script p2sh = ScriptBuilder.createP2SHOutputScript(redeemScript);
        // Format the p2sh (hash data) into an address.
        Address multisigAddress = Address.fromP2SHScript(params, p2sh);

        return new MultiSigAddress(multisigAddress,p2sh);
    }

    /**
     *
     * @param txId
     * @param redeemOutputs
     * @param to
     * @param toAmount
     * @param includeFee
     * @return
     */
    public Transaction createRawSpendTx(
            String txId,
            List<OutputWrapper> redeemOutputs,
            Address to,
            Coin toAmount,
            boolean includeFee
    ) throws Exception {

        Transaction tx = new Transaction(params);

        // Add Inputs
        tx = addInputs(tx, txId, redeemOutputs);

        // Add the output
        Script outputScript = ScriptBuilder.createOutputScript(to);
        if (includeFee){
            toAmount = toAmount.minus(Transaction.MIN_NONDUST_OUTPUT);
            if (toAmount.isNegative()){
                throw new Exception("Negative amount including fee");
            }
            tx.addOutput(toAmount, outputScript);
        }else
            tx.addOutput(toAmount, outputScript);

        return tx;
    }

    /**
     *
     * @param transaction
     * @param txId  ---> id of the tx in which the output to redeem lives.
     * @param redeemOutputs
     * @return
     */
    public Transaction addInputs(Transaction transaction, String txId, List<OutputWrapper> redeemOutputs){
        // The outputs that we want to redeem..
        for (OutputWrapper outputWrapper : redeemOutputs) {
            // Script which redeems an specific output.
            ScriptBuilder scriptSigBuilder = new ScriptBuilder();
            scriptSigBuilder.data(outputWrapper.getScriptBytes());
            transaction.addInput(Sha256Hash.wrap(txId),outputWrapper.getIndex(),scriptSigBuilder.build());
        }
        return transaction;
    }


    /**
     *
     * @param tx
     * //@param redeemScript
     * @param privKeys
     * @return
     * @throws Exception
     */
//    public Transaction signInputs(Transaction tx,final Script redeemScript,final List<ECKey> privKeys) throws Exception {
//        return signInputsWithoutOutpoints(
//                tx,
//                Lists.newArrayList(redeemScript),
//                privKeys);
//    }
//
//    /**
//     * TODO: El problema es que tengo que encontrar el script en base al output con el index + txId.
//     * @param tx
//     * @param redeemScripts
//     * @param privKeys
//     * @return
//     * @throws Exception
//     */
//    public Transaction signInputsWithoutOutpoints(Transaction tx,@Nullable final List<Script> redeemScripts, final List<ECKey> privKeys) throws Exception {
//        List<OutputWrapper> redeemOutpoints = new ArrayList<>();
//        for (Script redeemScript : redeemScripts) {
//            redeemOutpoints.add(
//                    new OutputWrapper(
//
//                    )
//            );
//        }
//        return signInputs(tx, redeemOutpoints, privKeys);
//    }

    public Transaction signInputs(Transaction tx, @Nullable final List<OutputWrapper> outpoints, final List<ECKey> privKeys) throws Exception {

        // Keys chain
        BasicKeyChain chain = new BasicKeyChain();
        chain.importKeys(privKeys);

        // Load the key bag
        CustomKeyBag keyBag = new CustomKeyBag(chain, outpoints);

        return signInputs(tx,keyBag);
    }

    /**
     *
     * @param tx
     * @param keyBag
     * @return
     * @throws Exception
     */
    public Transaction signInputs(Transaction tx, CustomKeyBag keyBag) throws Exception {

        MyTransactionSigner transactionSigner = new MyTransactionSigner();

        // Look for every input and try to sign it
        if (!transactionSigner.signInputs(tx,keyBag)){
            throw new Exception("{} returned false for the tx, " + transactionSigner.getClass().getName());
        }

        return tx;
    }



}
