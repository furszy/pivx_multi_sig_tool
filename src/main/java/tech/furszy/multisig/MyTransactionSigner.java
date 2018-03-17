package tech.furszy.multisig;

import org.pivxj.core.ECKey;
import org.pivxj.core.Sha256Hash;
import org.pivxj.core.Transaction;
import org.pivxj.core.TransactionInput;
import org.pivxj.crypto.TransactionSignature;
import org.pivxj.script.Script;
import org.pivxj.script.ScriptBuilder;
import org.pivxj.script.ScriptChunk;
import org.pivxj.wallet.KeyBag;
import org.pivxj.wallet.RedeemData;
import org.spongycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class MyTransactionSigner {



    /**
     * TODO: This is bad, this could have more than one redeem script..
     */
    public MyTransactionSigner() throws Exception {
        // Check if the script is a multiSig script
        //if (!redeemScript.isSentToMultiSig()) throw new Exception("Only multiSig scripts accepted");
    }

    /**
     *
     * @param propTx
     * @param keyBag
     * @return
     */
    public boolean signInputs(Transaction propTx, CustomKeyBag keyBag){

        /**
         * To sign the inputs of a multiSig address
         *
         *
         */

        for (int i = 0; i < propTx.getInputs().size(); i++) {
            TransactionInput input = propTx.getInput(i);

            // Get the scriptSig
            Script inputScript = input.getScriptSig();

            /*
             * For each input check if we have to sign them with the keys
             *
             * Create a list of all signatures. Start by extracting the existing ones from the list of script schunks.
             * The last signature in the script chunk list is the redeemScript
             *
             */
            List<TransactionSignature> signatureList = new ArrayList<TransactionSignature>();

            // Get the redeem script
            RedeemData redeemData = null;
            // todo: This is not finding the redeem script..
            if (input.getOutpoint().getConnectedOutput() != null) {
                redeemData = input.getConnectedRedeemData(keyBag);
            }else {
                redeemData = keyBag.findRedeemDataFromOutpoint(input.getOutpoint());
            }

            // Extract the previos signatures..
            // The last signature in the script chunk list is the redeemScript

            List<ScriptChunk> scriptChunks = inputScript.getChunks();
            Iterator<ScriptChunk> iterator = scriptChunks.iterator();

            Script redeemScript = null;
            if (redeemData != null){
                redeemScript = redeemData.redeemScript;
            }

            while (iterator.hasNext()) {
                ScriptChunk chunk = iterator.next();

                if (iterator.hasNext() && chunk.opcode != 0) {
                    TransactionSignature transactionSignarture = TransactionSignature.decodeFromBitcoin(chunk.data, false, false);
                    signatureList.add(transactionSignarture);
                } else {
                    if (redeemScript == null && !iterator.hasNext())
                        redeemScript = new Script(chunk.data);
                }
            }


            for (ECKey key : redeemScript.getPubKeys()) {

                ECKey fullKey = keyBag.findKeyFromPubKey(key.getPubKey());
                if (fullKey != null){
                    // Sign if we have the private key
                    Sha256Hash sighash = propTx.hashForSignature(i, redeemScript, Transaction.SigHash.ALL, false);
                    // sign the signhash
                    ECKey.ECDSASignature signature = fullKey.sign(sighash);
                    // Add the  signature to the signature list
                    TransactionSignature transactionSignarture = new TransactionSignature(signature, Transaction.SigHash.ALL, false);
                    signatureList.add(transactionSignarture);
                }else
                    continue;


            }

            // Rebuild p2sh multisig input script
            inputScript = ScriptBuilder.createP2SHMultiSigInputScript(signatureList, redeemScript);
            propTx.getInput(i).setScriptSig(inputScript);

            //Sha256Hash sighash = propTx.hashForSignature(i, redeemData.redeemScript, Transaction.SigHash.ALL, false);
            System.out.println("Input_"+i+": " + input);

        }


        return true;
    }

}
