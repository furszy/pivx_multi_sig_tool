package tech.furszy;

import org.pivxj.core.*;
import org.pivxj.crypto.TransactionSignature;
import org.pivxj.params.MainNetParams;
import org.pivxj.params.TestNet3Params;
import org.pivxj.script.Script;
import org.pivxj.script.ScriptBuilder;
import org.pivxj.script.ScriptChunk;
import org.spongycastle.util.encoders.Hex;
import tech.furszy.multisig.MultiSigAddress;
import tech.furszy.multisig.MultiSigBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Fast tool to create and redeem multi-sig addresses.
 */

public class Main {

    public static NetworkParameters params;


    // Networks
    public static final String MAINNET = "-mainNet";
    public static final String TESTNET = "-testNet";

    // Actions
    public static final String GENERATE_MULTI_SIG = "-gen";
    public static final String CREATE_SPEND_MULTI_SIG_TX = "-createTx";
    public static final String SIGN_MULTI_SIG_TX = "-sign";
    public static final String HELP = "-help";

    // Keys
    public static final String PUB_KEYS = "-pubKeys";
    public static final String KEY = "-key";

    // Redeem transaction information
    public static final String REDEEM_SCRIPT = "-redeemScript";
    public static final String REDEEM_OUTPUT_HEX = "-redeemOutputHex";
    public static final String REDEEM_OUTPUT_INDEX = "-redeemOutputIndex";
    public static final String REDEEM_OUTPUT_TX_HASH = "-redeemOutputTxHash";

    public static final String INCLUDE_FEE = "-includeFee";

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

            boolean includeFee = false;

            // Hex tx
            String hexTx = null;

            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                switch (arg) {

                    case HELP:
                        printHelp(genMultiSig, signTx, createTx);
                        System.exit(0);
                        break;
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
                        if (args.length > i + 1) {
                            String pubKeysStr = args[i + 1];
                            pubKeys = pubKeysStr.split(",");
                            if (pubKeys.length < 2){
                                System.out.println(KEY+" needs at least 2 pub keys");
                                System.exit(1);
                            }
                        }else {
                            System.out.println(KEY+" needs at least 2 pub keys");
                            System.exit(1);
                        }
                        break;
                    case KEY:
                        // KeyPair in hex form: [priv,pub]
                        String[] keyPair = args[i+1].split(",");
                        key = DumpedPrivateKey.fromBase58(params,keyPair[0]).getKey();
                        break;

                    // Redeem
                    case REDEEM_SCRIPT:
                        redeemScript = new Script(Hex.decode(args[i+1]));
                        break;
                    case REDEEM_OUTPUT_HEX:
                        redeemOutputHex = args[i+1];
                        break;
                    case REDEEM_OUTPUT_INDEX:
                        redeemOutputIndex = Integer.parseInt(args[i+1]);
                        break;
                    case REDEEM_OUTPUT_TX_HASH:
                        redeemOutputTxHash = args[i+1];
                        break;

                    case INCLUDE_FEE:
                        includeFee = true;
                        break;
                    // Sign
                    case TX_HEX:
                        hexTx = args[i+1];
                        break;

                    // Output fields
                    case ADDRESS_TO:
                        addressTo = args[i+1];
                        break;
                    case AMOUNT:
                        amount = Coin.parseCoin(args[i+1]);
                        break;

                }
            }

            // Check params
            if (params == null) {
                // If there is no params use mainnet.
                params = MainNetParams.get();
            }
            Context.getOrCreate(params);

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
                createMultiSig(pubKeys.length,pubKeys);
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

                createFirstSign(key, redeemScript, redeemOutputHex, redeemOutputIndex, redeemOutputTxHash, addressTo, amount, includeFee);
            }

        }catch (Exception e){
            e.printStackTrace();
            System.out.println(e.getMessage());
            System.exit(1);
        }
    }

    private static void printHelp(boolean genMultiSig, boolean signTx, boolean createTx) {
        if (genMultiSig){
            System.out.println(
                    GENERATE_MULTI_SIG+": is for create a multiSig address with 2 public keys\n"+
                            "Example:\n " +
                            "java -jar pivx-multi-sig.jar "+GENERATE_MULTI_SIG+" "+PUB_KEYS+" 023910b54c9ee1ab2570efc5ef25b93139cd81c25780b35ea2b9a088b5d3557ae7,023910b54c9ee1ab2570efc5ef25b93139cd81c25780b35ea2b9a088b5d3557ae7\n"

            );
        }else if (signTx){
            System.out.println(
                    SIGN_MULTI_SIG_TX+": is for sign a previously created raw hex transaction with the second private key\n"+
                            "Example:\n " +
                            "java -jar pivx-multi-sig.jar "+SIGN_MULTI_SIG_TX+" "+TX_HEX+" [raw_hex_tx] "+" "+KEY+" [priv_key,pub_key] "+"\n"
            );
        }else if (createTx){
            System.out.println(
                    CREATE_SPEND_MULTI_SIG_TX+": is for create a transaction and sign it with the first private key\n"+
                            "Example:\n " +
                            "java -jar pivx-multi-sig.jar "+CREATE_SPEND_MULTI_SIG_TX+" "+KEY+" [priv_key,pub_key] "+
                            REDEEM_SCRIPT +" [redeem_script_hex] "+REDEEM_OUTPUT_HEX + " [outputHex] "+
                            REDEEM_OUTPUT_INDEX + " outputIndex " + REDEEM_OUTPUT_TX_HASH + " [outputTxHash] " +
                            ADDRESS_TO +" [address] " + AMOUNT + " [long_amount] "
                            +"\n"
            );
        }else {
            System.out.println(

                    "Welcome to the PIVX tool for MultiSig transaction creation powered by Furszy!\n\n"+
                            "The commands are the following:\n"+
                            GENERATE_MULTI_SIG+": is for create a multiSig address with 2 public keys\n"+
                            CREATE_SPEND_MULTI_SIG_TX+": is for create a transaction and sign it with the first private key\n"+
                            SIGN_MULTI_SIG_TX+": is for sign a previously created raw hex transaction with the second private key\n"

            );
        }
    }

    public static void createMultiSig(int threshold,String[] pubKeys) {
        MultiSigBuilder multiSigBuilder = new MultiSigBuilder(params);
        MultiSigAddress multiSigAddress = multiSigBuilder.createMultiSigAddress(threshold,pubKeys);
        // Print the scripthash, this is used later to redeem the tokens..
        System.out.println("Redeem script: " + Hex.toHexString(multiSigAddress.getRedeemScript().getProgram()));
        System.out.println("MultiSig address: "+multiSigAddress.toBase58());
    }

    /**
     *
     * @param key1
     * @param redeemScript
     * @param redeemOutputHex
     * @param redeemOutputIndex
     * @param txId
     * @param addressTo
     * @param amount
     */
    public static String createFirstSign(ECKey key1, Script redeemScript, String redeemOutputHex, int redeemOutputIndex, String txId, String addressTo, Coin amount, boolean includeFee) throws Exception {
        // Start building the transaction by adding the unspent inputs we want to use
        Transaction spendTx = new Transaction(params);
        ScriptBuilder scriptBuilder = new ScriptBuilder();
        // The output that we want to redeem..
        scriptBuilder.data(redeemOutputHex.getBytes()); // Script of this output
        // tx hash
        TransactionInput input = spendTx.addInput(Sha256Hash.wrap(txId), redeemOutputIndex, scriptBuilder.build());

        System.out.println("Input"+": " + input);

        // Add outputs to the person receiving pivx
        Address receiverAddress = Address.fromBase58(params, addressTo);
        Script outputScript = ScriptBuilder.createOutputScript(receiverAddress);
        if (includeFee){
            amount = amount.minus(Transaction.MIN_NONDUST_OUTPUT);
            if (amount.isNegative()){
                throw new Exception("Negative amount including fee");
            }
            spendTx.addOutput(amount, outputScript);
        }else
            spendTx.addOutput(amount, outputScript);

        // Sign the first part of the transaction using private key #1
        Sha256Hash sighash = spendTx.hashForSignature(0, redeemScript, Transaction.SigHash.ALL, false);
        ECKey.ECDSASignature ecdsaSignature = key1.sign(sighash);
        TransactionSignature transactionSignarture = new TransactionSignature(ecdsaSignature, Transaction.SigHash.ALL, false);

        // Create p2sh multisig input script
        Script inputScript = ScriptBuilder.createP2SHMultiSigInputScript(Arrays.asList(transactionSignarture), redeemScript);

        // Add the script signature to the input
        input.setScriptSig(inputScript);

        System.out.println("Input signed: " + input);

        String rawTx = Hex.toHexString(spendTx.bitcoinSerialize());
        System.out.println("Transaction: \n "+ spendTx.toString());
        System.out.println("\n\n Hex Value: \n");
        System.out.println(rawTx);

        return rawTx;
    }

    /**
     *
     * @param txStr
     * @param key2
     */
    static public String signWithSecondKey(String txStr, ECKey key2){

        Transaction spendTx = new Transaction(params,Hex.decode(txStr));

        System.out.println("-----------");
        System.out.println(spendTx);
        System.out.println("---------");

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

        // Check if the script is ok.
        //inputScript.correctlySpends(spendTx,0,);

        spendTx.getInput(0).setScriptSig(inputScript);

        String rawTx = Hex.toHexString(spendTx.bitcoinSerialize());

        System.out.println("----------------");
        System.out.println("Final tx: "+spendTx);
        System.out.println("----------------");
        System.out.println("Hex signed transaction: " + rawTx);

        return rawTx;
    }

}
