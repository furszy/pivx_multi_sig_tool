import org.junit.Test;
import org.pivxj.core.*;
import org.pivxj.params.MainNetParams;
import org.pivxj.script.Script;
import org.pivxj.script.ScriptBuilder;
import org.pivxj.script.ScriptChunk;
import org.spongycastle.util.encoders.Hex;
import tech.furszy.Main;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class BasicTests {

    NetworkParameters params;


    public void setup(){
        params = MainNetParams.get();
        Context.getOrCreate(params);
    }


    @Test
    public void createMultiSig(){

        setup();

        List<String> pubKeys = new ArrayList<>();

        // Address 1 --> ﻿DHWKvZZxcg3rBR3m9RkaEGYD6Pkb5x61Ea
        pubKeys.add("03769190e94f3d695037217585892719d3cf6f2b4f54a046623d7ef6ba03af97ed");
        // Address 2 --> DJcvCWwEwCSSUY7vQiXw4aTmqULKBDRTPb
        pubKeys.add("03533cfc0656b8fb69104b80b98be68179678e96f16a8b4f8cee6d607449a3c2f6");


        List<ECKey> keys = new ArrayList<>();

        for (String pubKey : pubKeys) {
            keys.add(
                    ECKey.fromPublicOnly(Hex.decode(pubKey))
            );
        }

        // Create a multisig output script.
        Script redeemScript = ScriptBuilder.createMultiSigOutputScript(2, keys);
        // Print the scripthash, this is used later to redeem the tokens..
        List<ScriptChunk> list = redeemScript.getChunks();
        for (int i = 0; i < list.size(); i++) {
            System.out.println("chunk "+i+" "+list.get(i));
        }
        System.out.println("Redeem program: "+ Arrays.toString(redeemScript.getProgram()));
        System.out.println("Redeem script: " + Hex.toHexString(redeemScript.getProgram()));

        // Creates a Pat to Script Hash script to minimize the amount of data and obfuscate it in the blockchain.
        // TODO: Read bip 16
        Script script = ScriptBuilder.createP2SHOutputScript(redeemScript);

        // Creates an address from the 20 bytes of the data in the P2SH script, starting with the network version.
        Address address = Address.fromP2SHScript(params,script);

        System.out.println("New address: "+address.toBase58());

        /**
         *
         * Redeem script
         * 522103769190e94f3d695037217585892719d3cf6f2b4f54a046623d7ef6ba03af97ed2103533cfc0656b8fb69104b80b98be68179678e96f16a8b4f8cee6d607449a3c2f652ae
         *
         * Detailed:
         *
         *  52  21 03769190e94f3d695037217585892719d3cf6f2b4f54a046623d7ef6ba03af97ed 21 03533cfc0656b8fb69104b80b98be68179678e96f16a8b4f8cee6d607449a3c2f6  52   ae
         * ---- -- ------------------------------------------------------------------ -- ------------------------------------------------------------------ ---- ----------------
         * OP_2 33                       1 pub key                                    33                             2 pub key                              OP_2 OP_CHECKMULTISIG
         */
    }

    @Test
    public void testCreateTx(){
        setup();
        try {
            Main.params = params;
            ECKey ecKey = DumpedPrivateKey.fromBase58(params, "YRFbkUC2F8QTmwC7FEzw1VNSkjdcSDimYg3h1wZs4p7NibLSkaKW").getKey();
            Script redeemScript = new Script(Hex.decode("522102eed43149a2d0d681ceec269ff64f0380ce011f3d42fdaf47b0cc9b9ff0944c802103fb8ea7eb154134e796d68cf5ac24aff7f9e0c89be91315338acc6b995b8e174552ae"));
            String redeemOutputHex = "a91418f6064bad8443f505234c6bc58b17e8fd450a9787";
            int redeemOutputIndex = 1;
            String txId = "4ae356440f5281c01c1dda8d552b02c6df665d42cf1d73082f35091e8d9387f0";
            String toAddress = "DE3B8sSewkziSnuBrLT6Rpr3xeVUCwrrTW";
            Coin amount = Coin.parseCoin("0.03000000");
            String rawTxHex = Main.createFirstSign(
                    ecKey,
                    redeemScript,
                    redeemOutputHex,
                    redeemOutputIndex,
                    txId,
                    toAddress,
                    amount,
                    true
            );


            ECKey ecKey2 = DumpedPrivateKey.fromBase58(params,"YScdmzLgvJSL2SLG3VcZL2U6y2UryqteUr1GUnXob7wvGK4pmrTx").getKey();
            String rawTx = Main.signWithSecondKey(rawTxHex,ecKey2);


        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Test
    public void signTx(){
        setup();
        Main.params = params;
        String rawTxHex = "0100000001e77e2f078ba7e101f4789c6dd58c3fbeef2befc0397d1428c46ec4aa461dd3210000000092004830450221008f57392a47fcf3714173174158214fa678f67a913f073d1c0475fdd1eb802c9c0220021017efb46b1d814342c9ee5d78219be59e459c70ce59a024d07534bb18539b0147522102eed43149a2d0d681ceec269ff64f0380ce011f3d42fdaf47b0cc9b9ff0944c802103fb8ea7eb154134e796d68cf5ac24aff7f9e0c89be91315338acc6b995b8e174552aeffffffff01dadf0e00000000001976a914619a793503442ed33c41df7cf06299ea63b2f0a188ac00000000";
        ECKey ecKey = DumpedPrivateKey.fromBase58(params,"YScdmzLgvJSL2SLG3VcZL2U6y2UryqteUr1GUnXob7wvGK4pmrTx").getKey();
        Main.signWithSecondKey(rawTxHex,ecKey);
    }

}
