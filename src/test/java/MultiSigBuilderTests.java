import com.google.common.collect.Lists;
import org.junit.Test;
import org.pivxj.core.*;
import org.pivxj.params.MainNetParams;
import org.pivxj.script.Script;
import org.spongycastle.util.encoders.Hex;
import tech.furszy.multisig.MultiSigBuilder;
import tech.furszy.multisig.OutputWrapper;

import java.util.Arrays;
import java.util.List;

public class MultiSigBuilderTests {

    @Test
    public void createAndSignTest(){

        try {

            NetworkParameters params = MainNetParams.get();
            Context.getOrCreate(params);

            MultiSigBuilder multiSigBuilder = new MultiSigBuilder(params);

            // Redeem data.
            String hexRedeemScript = "522102eed43149a2d0d681ceec269ff64f0380ce011f3d42fdaf47b0cc9b9ff0944c802103fb8ea7eb154134e796d68cf5ac24aff7f9e0c89be91315338acc6b995b8e174552ae";

            // Output to redeem info.
            String redeemOutputHex = "a91418f6064bad8443f505234c6bc58b17e8fd450a9787";
            int redeemOutputIndex = 1;
            String txId = "4ae356440f5281c01c1dda8d552b02c6df665d42cf1d73082f35091e8d9387f0";

            // Amount to send.
            String toAddress = "DE3B8sSewkziSnuBrLT6Rpr3xeVUCwrrTW";
            Coin amount = Coin.parseCoin("0.03000000");

            Script redeemScript = new Script(Hex.decode(hexRedeemScript));

            List<OutputWrapper> outPoints = Lists.newArrayList(
                    new OutputWrapper(
                            redeemOutputIndex,
                            redeemOutputHex.getBytes(),
                            Sha256Hash.wrap(txId),
                            redeemScript
                    )
            );

            Transaction tx = multiSigBuilder.createRawSpendTx(
                    outPoints,
                    Address.fromBase58(params, toAddress),
                    amount,
                    true
            );


            // Signing keys:
            ECKey ecKey = DumpedPrivateKey.fromBase58(params, "YRFbkUC2F8QTmwC7FEzw1VNSkjdcSDimYg3h1wZs4p7NibLSkaKW").getKey();

            List<ECKey> keys = Lists.newArrayList(ecKey);

            tx = multiSigBuilder.signInputs(tx, outPoints, keys);

            System.out.println("########### Partial tx #############");
            System.out.println(tx);
            System.out.println("########### End of parcial tx ############");


            // #### Now sign it with the second key

            // Second key
            ECKey ecKey2 = DumpedPrivateKey.fromBase58(params,"YScdmzLgvJSL2SLG3VcZL2U6y2UryqteUr1GUnXob7wvGK4pmrTx").getKey();

            // Serialize the tx to create the second device context information
            String hexTx = Hex.toHexString(tx.bitcoinSerialize());

            Transaction fullTx = multiSigBuilder.signInputs(
                     new Transaction(params,Hex.decode(hexTx)),
                     null,
                     Arrays.asList(ecKey2)
             );

            System.out.println("######## Full tx ##########");
            System.out.println(fullTx);
            System.out.println("########## End of Full Tx  #############");


        } catch (Exception e) {
            e.printStackTrace();
        }

    }


}
