import org.junit.Test;
import org.pivxj.core.*;
import org.pivxj.params.MainNetParams;
import org.pivxj.script.Script;
import org.spongycastle.util.encoders.Hex;
import tech.furszy.Main;



public class BasicTests {

    NetworkParameters params;


    public void setup(){
        params = MainNetParams.get();
        Context.getOrCreate(params);
    }

    @Test
    public void testCreateTx(){
        setup();
        try {
            Main.params = params;
            ECKey ecKey = DumpedPrivateKey.fromBase58(params, "YRFbkUC2F8QTmwC7FEzw1VNSkjdcSDimYg3h1wZs4p7NibLSkaKW").getKey();
            Script redeemScript = new Script(Hex.decode("522102eed43149a2d0d681ceec269ff64f0380ce011f3d42fdaf47b0cc9b9ff0944c802103fb8ea7eb154134e796d68cf5ac24aff7f9e0c89be91315338acc6b995b8e174552ae"));
            String redeemOutputHex = "76a9140375449b8b6a1bfabc8b02c1b4f821a9cef71bdd88ac";
            int redeemOutputIndex = 0;
            String redeemOutputTxHash = "21d31d46aac46ec428147d39c0ef2befbe3f8cd56d9c78f401e1a78b072f7ee7";
            String toAddress = "DE3B8sSewkziSnuBrLT6Rpr3xeVUCwrrTW";
            Coin amount = Coin.parseCoin("0.01004810");
            Main.createFirstSign(
                    ecKey,
                    redeemScript,
                    redeemOutputHex,
                    redeemOutputIndex,
                    redeemOutputTxHash,
                    toAddress,
                    amount,
                    true
            );
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
