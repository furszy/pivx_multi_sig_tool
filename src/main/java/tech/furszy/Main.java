package tech.furszy;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.pivxj.core.*;
import org.pivxj.params.MainNetParams;
import org.pivxj.params.TestNet3Params;
import org.pivxj.script.Script;
import org.spongycastle.util.encoders.Hex;
import tech.furszy.multisig.MultiSigAddress;
import tech.furszy.multisig.MultiSigBuilder;
import tech.furszy.multisig.OutputWrapper;

import java.util.ArrayList;
import java.util.Arrays;
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
    public static final String KEYS = "-keys";

    // Redeem transaction information
    public static final String REDEEM_OUTPUTS = "-redeemOutputs";
    //public static final String REDEEM_SCRIPT = "-redeemScript";
    //public static final String REDEEM_OUTPUT_HEX = "-redeemOutputHex";
    //public static final String REDEEM_OUTPUT_INDEX = "-redeemOutputIndex";
    //public static final String REDEEM_OUTPUT_TX_HASH = "-redeemOutputParentTxId";

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

            List<ECKey> keys = new ArrayList<>();


            // Redeem outputs
            List<OutputWrapper> outPoints = new ArrayList<>();

            // TODO: Add several outputs possibility..
            String addressTo = null;
            Coin amount = null;

            boolean includeFee = true;

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
                                System.out.println(PUB_KEYS +" needs at least 2 pub keys");
                                System.exit(1);
                            }
                        }else {
                            System.out.println(PUB_KEYS +" needs at least 2 pub keys");
                            System.exit(1);
                        }
                        break;
                    case KEYS:
                        // KeyPair in hex form: [{\"priv\": \"priv\",\"pub\":\"pub\"},{\"priv\": \"priv\",\"pub\":\"pub\"}]
                        /**
                         * -createTx -keys [{\"priv\":\"YRFbkUC2F8QTmwC7FEzw1VNSkjdcSDimYg3h1wZs4p7NibLSkaKW\"}] -toAddress "DE3B8sSewkziSnuBrLT6Rpr3xeVUCwrrTW" -amount 0.03000000  -redeemOutputs                                 "[{\"index\": 1 , \"parentTxId\": \"4ae356440f5281c01c1dda8d552b02c6df665d42cf1d73082f35091e8d9387f0\" , \"scriptBytesHex\": \"a91418f6064bad8443f505234c6bc58b17e8fd450a9787\" , \"redeemScriptHex\": \"522102eed43149a2d0d681ceec269ff64f0380ce011f3d42fdaf47b0cc9b9ff0944c802103fb8ea7eb154134e796d68cf5ac24aff7f9e0c89be91315338acc6b995b8e174552ae\"}]"
                         */
                        JSONArray jsonkeys = (JSONArray) new JSONParser().parse(args[i+1]);

                        for (int i1 = 0; i1 < jsonkeys.size(); i1++) {
                            JSONObject jsonKey = (JSONObject) jsonkeys.get(i1);
                            keys.add(
                                    DumpedPrivateKey.fromBase58(
                                            params,
                                            String.valueOf(jsonKey.get("priv"))
                                    ).getKey());
                        }
                        break;

                    // Redeem
                    case REDEEM_OUTPUTS:
                        JSONArray jsonArray = (JSONArray) new JSONParser().parse(args[i+1]);
                        // The structure is:
                        //  [
                        //    {"index": index , "parentTxId": txId , "scriptBytesHex": scryptBytes , "redeemScriptHex": redeemScript},
                        //    {"index": index , "parentTxId": txId , "scriptBytesHex": scryptBytes , "redeemScriptHex": redeemScript}
                        //  ]
                        //

                        for (int i1 = 0; i1 < jsonArray.size(); i1++) {
                            JSONObject jsonOutput = (JSONObject) jsonArray.get(i1);

                            long index = (long) jsonOutput.get("index");
                            byte[] scriptBytes = Hex.decode((String) jsonOutput.get("scriptBytesHex"));
                            String parentTxId = (String) jsonOutput.get("parentTxId");
                            String redeemScriptHex = (String) jsonOutput.get("redeemScriptHex");

                            outPoints.add(
                                    new OutputWrapper(
                                            index,
                                            scriptBytes,
                                            Sha256Hash.wrap(parentTxId),
                                            new Script(Hex.decode(redeemScriptHex))
                                    )
                            );

                        }

                        break;

                    case INCLUDE_FEE:
                        includeFee = Boolean.parseBoolean(args[i+1]);
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
                checkNotNull(keys, KEYS + " must not be null");
                checkNotNull(hexTx, TX_HEX + " must not be null");
                sign(hexTx, keys);
            } else if (createTx) {
                checkNotNull(keys, KEYS + " must not be null");
                //checkNotNull(redeemScript, REDEEM_SCRIPT + " must not be null");
                //checkNotNull(redeemOutputHex, REDEEM_OUTPUT_HEX + " must not be null");
                //checkNotNull(redeemOutputIndex, REDEEM_OUTPUT_INDEX + " must not be null");
                //checkNotNull(txId, REDEEM_OUTPUT_TX_HASH + " must not be null");

                checkNotNull(addressTo, ADDRESS_TO + " must not be null");
                checkNotNull(amount, AMOUNT + " must not be null");


                createFirstSign(keys, outPoints , addressTo, amount, includeFee);
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
                            "java -jar pivx-multi-sig.jar "+SIGN_MULTI_SIG_TX+" "+TX_HEX+" [raw_hex_tx] "+" "+ KEYS +" [{\"priv\": \"priv\",\"pub\":\"pub\"},{\"priv\": \"priv\",\"pub\":\"pub\"}] "+"\n"
            );
        }else if (createTx){
            System.out.println(
                    CREATE_SPEND_MULTI_SIG_TX+": is for create a transaction and sign it with the first private key\n"+
                            "Example:\n " +
                            "java -jar pivx-multi-sig.jar "+CREATE_SPEND_MULTI_SIG_TX+" "+ KEYS +" [{\"priv\": \"priv\",\"pub\":\"pub\"},{\"priv\": \"priv\",\"pub\":\"pub\"}] "+
                            REDEEM_OUTPUTS +" [json_redeem_outputs] "+
                            ADDRESS_TO +" [address] " + AMOUNT + " [long_amount] "
                            +"\n\n"+
                            "The json_redeem_outputs must be in the following structure:\n" +
                            " The structure is:\n" +
                            "[\n" +
                            "  {\"index\": index , \"parentTxId\": txId , \"scriptBytesHex\": scryptBytes , \"redeemScriptHex\": redeemScript},\n" +
                            "  {\"index\": index , \"parentTxId\": txId , \"scriptBytesHex\": scryptBytes , \"redeemScriptHex\": redeemScript}\n" +
                            "]"
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
     * @param keys
     * @param toAddress
     * @param toAmount
     */
    public static String createFirstSign(List<ECKey> keys, List<OutputWrapper> outPoints, String toAddress, Coin toAmount, boolean includeFee) throws Exception {

        MultiSigBuilder multiSigBuilder = new MultiSigBuilder(params);

        Transaction spendTx = multiSigBuilder.createRawSpendTx(
                outPoints,
                Address.fromBase58(params, toAddress),
                toAmount,
                includeFee
        );

        spendTx = multiSigBuilder.signInputs(
                spendTx,
                outPoints,
                keys
        );

        String rawTx = Hex.toHexString(spendTx.bitcoinSerialize());
        System.out.println("Transaction: \n "+ spendTx.toString());
        System.out.println("\n\n Hex Value: \n");
        System.out.println(rawTx);

        return rawTx;
    }

    /**
     *
     * @param txStr
     * @param keys
     */
    static public String sign(String txStr, List<ECKey> keys) throws Exception {

        Transaction spendTx = new Transaction(params,Hex.decode(txStr));

        System.out.println("-----------");
        System.out.println(spendTx);
        System.out.println("---------");

        MultiSigBuilder multiSigBuilder = new MultiSigBuilder(params);
        spendTx = multiSigBuilder.signInputs(
                spendTx,
                null,
                keys
        );

        String rawTx = Hex.toHexString(spendTx.bitcoinSerialize());

        System.out.println("----------------");
        System.out.println("Final tx: "+spendTx);
        System.out.println("----------------");
        System.out.println("Hex signed transaction: " + rawTx);

        return rawTx;
    }

}
