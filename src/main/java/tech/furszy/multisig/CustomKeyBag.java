package tech.furszy.multisig;

import com.sun.istack.internal.Nullable;
import org.pivxj.core.ECKey;
import org.pivxj.core.TransactionOutPoint;
import org.pivxj.script.Script;
import org.pivxj.script.ScriptBuilder;
import org.pivxj.wallet.BasicKeyChain;
import org.pivxj.wallet.KeyBag;
import org.pivxj.wallet.RedeemData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomKeyBag implements KeyBag {

    private BasicKeyChain basicKeyChain;
    private Map<byte[], Script> redeemScripts = new HashMap<>();
    private Map<OutputWrapper, Script> redeemScriptsByOutPoints = new HashMap<>();

    public CustomKeyBag(BasicKeyChain basicKeyChain, @Nullable List<OutputWrapper> outputWrappers){
        this.basicKeyChain = basicKeyChain;
        if (outputWrappers != null)
            for (OutputWrapper redeemOutpoint : outputWrappers) {
                // Add the ps2h to the first map
                if (redeemOutpoint.getRedeemScript() != null)
                    addRedeemScript(redeemOutpoint.getRedeemScript());
                if (redeemOutpoint.getTxId() != null) {
                    // Add the outpoint to the map
                    redeemScriptsByOutPoints.put(redeemOutpoint, redeemOutpoint.getRedeemScript());
                }
            }
    }

    private void addRedeemScript(Script redeemScript){
        byte[] key = null;
        if (redeemScript.isPayToScriptHash()) {
            key = redeemScript.getPubKeyHash();
        } else if (redeemScript.isSentToMultiSig()) {
            // If the script is a multiSig script i use the ps2h as key
            key = ScriptBuilder.createP2SHOutputScript(redeemScript).getPubKeyHash();
        }
        this.redeemScripts.put(
                key,
                redeemScript
        );
    }

    @Override
    public ECKey findKeyFromPubHash(byte[] bytes) {
        return basicKeyChain.findKeyFromPubHash(bytes);
    }

    @Override
    public ECKey findKeyFromPubKey(byte[] bytes) {
        return basicKeyChain.findKeyFromPubKey(bytes);
    }

    @Override
    public RedeemData findRedeemDataFromScriptHash(byte[] bytes) {
        return checkAndReturn(redeemScripts.get(bytes));
    }

    public RedeemData findRedeemDataFromOutpoint(TransactionOutPoint outPoint){
        return checkAndReturn(redeemScriptsByOutPoints.get(new OutputWrapper(outPoint.getIndex(), null,outPoint.getHash(),null)));
    }

    private RedeemData checkAndReturn(Script redeemScript){
        if (redeemScript == null) return null;
        return RedeemData.of(redeemScript.getPubKeys(), redeemScript);
    }
}