package tech.furszy.multisig;

import org.pivxj.core.Address;
import org.pivxj.script.Script;
import org.pivxj.script.ScriptBuilder;

public class MultiSigAddress {

    private Address multiSigAddress;
    private Script redeemScript;


    public MultiSigAddress(Address multiSigAddress, Script redeemScript) {
        this.multiSigAddress = multiSigAddress;
        this.redeemScript = redeemScript;
    }

    /**
     * The script pub key is a pay to script hash so what i have to do is create it.
     *
     * @return scriptPubKey
     */
    public Script getScriptPubKey() {
        return ScriptBuilder.createP2SHOutputScript(redeemScript);
    }

    public Address getMultiSigAddress() {
        return multiSigAddress;
    }

    public Script getRedeemScript() {
        return redeemScript;
    }

    public String toBase58() {
        return multiSigAddress.toBase58();
    }

    @Override
    public String toString(){
        return "Address: " + multiSigAddress.toBase58()+"\n"+
                "RedeemScript: " + redeemScript.toString();
    }
}