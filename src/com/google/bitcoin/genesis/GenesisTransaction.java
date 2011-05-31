package com.google.bitcoin.genesis;

import java.io.ByteArrayOutputStream;

import com.google.bitcoin.bouncycastle.util.encoders.Hex;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.Script;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionInput;
import com.google.bitcoin.core.TransactionOutput;

public class GenesisTransaction 
    extends Transaction{

    /**
     * 
     */
    private static final long serialVersionUID = 2765585823867164702L;

    GenesisTransaction(NetworkParameters params) {
        super(params);
        try {
            // A script containing the difficulty bits and
            // the following message:
            //
            // "The Times 03/Jan/2009 Chancellor on brink of second bailout for banks"
            byte[] bytes = Hex
                    .decode("04ffff001d0104455468652054696d65732030332f4a616e2f32303039204368616e63656c6c6f72206f6e206272696e6b206f66207365636f6e64206261696c6f757420666f722062616e6b73");
            inputs.add(new TransactionInput(params,this,bytes));
            ByteArrayOutputStream scriptPubKeyBytes = new ByteArrayOutputStream();
            Script.writeBytes(
                    scriptPubKeyBytes,
                    Hex.decode("04678afdb0fe5548271967f1a67130b7105cd6a828e03909a67962e0ea1f61deb649f6bc3f4cef38c4f35504e51ec112de5c384df7ba0b8d578a4c702b6bf11d5f"));
            scriptPubKeyBytes.write(Script.OP_CHECKSIG);
            outputs.add(new TransactionOutput(params,scriptPubKeyBytes
                    .toByteArray()));
        } catch (Exception e) {
            // Cannot happen.
        }
    }

}
