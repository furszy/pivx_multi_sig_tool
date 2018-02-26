package tech.furszy;

import org.pivxj.core.*;
import org.pivxj.crypto.TransactionSignature;
import org.pivxj.params.MainNetParams;
import org.pivxj.params.TestNet3Params;
import org.pivxj.script.Script;
import org.pivxj.script.ScriptBuilder;
import org.pivxj.script.ScriptChunk;
import org.spongycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Fast tool to create and redeem multi-sig addresses.
 */

public class Main {

    private static NetworkParameters params;


    // Networks
    public static final String MAINNET = "-mainNet";
    public static final String TESTNET = "-testNet";

    // Actions
    public static final String GENERATE_MULTI_SIG = "-gen";
    public static final String CREATE_SPEND_MULTI_SIG_TX = "-createTx";
    public static final String SIGN_MULTI_SIG_TX = "-sign";

    // Keys
    public static final String PUB_KEYS = "-pubKeys";
    public static final String KEY = "-key";

    // Redeem transaction information
    public static final String REDEEM_SCRIPT = "-redeemScript";
    public static final String REDEEM_OUTPUT_HEX = "-redeemOutputHex";
    public static final String REDEEM_OUTPUT_INDEX = "-redeemOutputIndex";
    public static final String REDEEM_OUTPUT_TX_HASH = "-redeemOutputTxHash";

    // Sign
    public static final String TX_HEX = "-rawTxHex";

    // Transaction output fields
    public static final String ADDRESS_TO = "-toAddress";
    public static final String AMOUNT = "-amount";



    public static void main(String[] args){

        try {

            boolean genMultiSig = false;
            boolean signTx = false;
            boolean createTx = false;

            String[] pubKeys = null;

            ECKey key = null;

            // Create tx and sign it.
            Script redeemScript = null;
            String redeemOutputHex = null;
            int redeemOutputIndex = -1;
            String redeemOutputTxHash = null;
            String addressTo = null;
            Coin amount = null;

            // Hex tx
            String hexTx = null;

            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                switch (arg) {
                    // Network
                    case MAINNET:
                        params = MainNetParams.get();
                        break;
                    case TESTNET:
                        params = TestNet3Params.get();
                        break;
                    // Actions
                    case GENERATE_MULTI_SIG:
                        genMultiSig = true;
                        break;
                    case SIGN_MULTI_SIG_TX:
                        signTx = true;
                        break;
                    case CREATE_SPEND_MULTI_SIG_TX:
                        createTx = true;
                        break;
                    // Key check
                    case PUB_KEYS:
                        pubKeys = args[i + 1].split(",");
                        break;
                    case KEY:
                        // KeyPair in hex form: [priv,pub]
                        String[] keyPair = arg.split(",");
                        key = new ECKey(Hex.decode(keyPair[0]), Hex.decode(keyPair[1]));
                        break;

                    // Redeem
                    case REDEEM_SCRIPT:
                        redeemScript = new Script(Hex.decode(arg));
                        break;
                    case REDEEM_OUTPUT_HEX:
                        redeemOutputHex = arg;
                        break;
                    case REDEEM_OUTPUT_INDEX:
                        redeemOutputIndex = Integer.parseInt(arg);
                        break;
                    case REDEEM_OUTPUT_TX_HASH:
                        redeemOutputTxHash = arg;
                        break;

                    // Sign
                    case TX_HEX:
                        hexTx = arg;
                        break;

                    // Output fields
                    case ADDRESS_TO:
                        addressTo = arg;
                        break;
                    case AMOUNT:
                        amount = Coin.parseCoin(arg);
                        break;

                }
            }

            // Check params
            if (params == null) {
                // If there is no params use mainnet.
                params = MainNetParams.get();
            }

            if (!genMultiSig && !signTx && !createTx) {
                System.out.println("-gen or -sign or -createtx must be used.. please check instructions");
                System.exit(1);
            }

            if (genMultiSig && signTx) {
                System.out.println("-gen and -sign cannot be used together.. please check instructions");
                System.exit(1);
            }

            if (genMultiSig) {
                checkNotNull(pubKeys, PUB_KEYS + " must not be null");
                System.out.println("Using pub keys: " + Arrays.toString(pubKeys));
                createMultiSig(pubKeys);
            } else if (signTx) {
                checkNotNull(key, KEY + " must not be null");
                checkNotNull(hexTx, TX_HEX + " must not be null");
                signWithSecondKey(hexTx, key);
            } else if (createTx) {
                checkNotNull(key, KEY + " must not be null");
                checkNotNull(redeemScript, REDEEM_SCRIPT + " must not be null");
                checkNotNull(redeemOutputHex, REDEEM_OUTPUT_HEX + " must not be null");
                checkNotNull(redeemOutputIndex, REDEEM_OUTPUT_INDEX + " must not be null");
                checkNotNull(redeemOutputTxHash, REDEEM_OUTPUT_TX_HASH + " must not be null");
                checkNotNull(addressTo, ADDRESS_TO + " must not be null");
                checkNotNull(amount, AMOUNT + " must not be null");

                createFirstSign(key, redeemScript, redeemOutputHex, redeemOutputIndex, redeemOutputTxHash, addressTo, amount);
            }

        }catch (Exception e){
            System.out.println(e.getMessage());
            System.exit(1);
        }
    }

    private static void createMultiSig(String[] pubKeys) {

        List<ECKey> keys = new ArrayList<>();

        for (String pubKey : pubKeys) {
            keys.add(
                    ECKey.fromPublicOnly(Hex.decode(pubKey))
            );
        }

        // Create a multisig output script.
        Script redeemScript = ScriptBuilder.createMultiSigOutputScript(pubKeys.length, keys);
        // Print the scripthash, this is used later to redeem the tokens..
        System.out.println("Redeem script: " + Hex.toHexString(redeemScript.getProgram()));
        Script script = ScriptBuilder.createP2SHOutputScript(redeemScript);

        Address multisigAddress = Address.fromP2SHScript(params, script);
        System.out.println("MultiSig address: "+multisigAddress.toBase58());
    }

    /**
     *
     * @param key1
     * @param redeemScript
     * @param redeemOutputHex
     * @param redeemOutputIndex
     * @param redeemOutputTxHash
     * @param addressTo
     * @param amount
     */
    private static void createFirstSign(ECKey key1, Script redeemScript, String redeemOutputHex, int redeemOutputIndex, String redeemOutputTxHash, String addressTo, Coin amount){
        // Start building the transaction by adding the unspent inputs we want to use
        Transaction spendTx = new Transaction(params);
        ScriptBuilder scriptBuilder = new ScriptBuilder();
        // The output that we want to redeem..
        scriptBuilder.data(redeemOutputHex.getBytes()); // Script of this output
        // tx hash
        String txHash = redeemOutputTxHash;
        TransactionInput input = spendTx.addInput(Sha256Hash.wrap(txHash), 1, scriptBuilder.build());

        // Add outputs to the person receiving pivx
        Address receiverAddress = Address.fromBase58(params, addressTo);
        Script outputScript = ScriptBuilder.createOutputScript(receiverAddress);
        spendTx.addOutput(amount, outputScript);

        // Sign the first part of the transaction using private key #1
        Sha256Hash sighash = spendTx.hashForSignature(redeemOutputIndex, redeemScript, Transaction.SigHash.ALL, false);
        ECKey.ECDSASignature ecdsaSignature = key1.sign(sighash);
        TransactionSignature transactionSignarture = new TransactionSignature(ecdsaSignature, Transaction.SigHash.ALL, false);

        // Create p2sh multisig input script
        Script inputScript = ScriptBuilder.createP2SHMultiSigInputScript(Arrays.asList(transactionSignarture), redeemScript);

        // Add the script signature to the input
        input.setScriptSig(inputScript);
        System.out.println(Hex.toHexString(spendTx.bitcoinSerialize()));
    }

    /**
     *
     * @param txStr
     * @param key2
     */
    static private void signWithSecondKey(String txStr, ECKey key2){

        Transaction spendTx = new Transaction(params,Hex.decode(txStr));

        // Get the input chunks
        Script inputScript = spendTx.getInput(0).getScriptSig();
        List<ScriptChunk> scriptChunks = inputScript.getChunks();

        // Create a list of all signatures. Start by extracting the existing ones from the list of script schunks.
        // The last signature in the script chunk list is the redeemScript
        List<TransactionSignature> signatureList = new ArrayList<TransactionSignature>();
        Iterator<ScriptChunk> iterator = scriptChunks.iterator();
        Script redeemScript = null;

        while (iterator.hasNext())
        {
            ScriptChunk chunk = iterator.next();

            if (iterator.hasNext() && chunk.opcode != 0)
            {
                TransactionSignature transactionSignarture = TransactionSignature.decodeFromBitcoin(chunk.data, false);
                signatureList.add(transactionSignarture);
            } else
            {
                redeemScript = new Script(chunk.data);
            }
        }

        // Create the sighash using the redeem script
        Sha256Hash sighash = spendTx.hashForSignature(0, redeemScript, Transaction.SigHash.ALL, false);
        ECKey.ECDSASignature secondSignature;

        // sign the signhash
        secondSignature = key2.sign(sighash);

        // Add the second signature to the signature list
        TransactionSignature transactionSignarture = new TransactionSignature(secondSignature, Transaction.SigHash.ALL, false);
        signatureList.add(transactionSignarture);

        // Rebuild p2sh multisig input script
        inputScript = ScriptBuilder.createP2SHMultiSigInputScript(signatureList, redeemScript);
        spendTx.getInput(0).setScriptSig(inputScript);

        System.out.println("Hex signed transaction: " + Hex.toHexString(spendTx.bitcoinSerialize()));
    }

}
