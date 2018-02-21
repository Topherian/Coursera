package scroogeCoin;

import java.security.PublicKey;
import java.util.ArrayList;

public class TxHandler {

    private final UTXOPool utxoPool;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        // IMPLEMENT THIS
        this.utxoPool = new UTXOPool(utxoPool);

    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool,
     * (2) the signatures on each input of {@code tx} are valid,
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     * values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        return (checkOutputs(tx) &&
                checkSignatures(tx) &&
                !isUTXOClaimedMultipleTimes(tx) &&
                isTransactionOutputsAllNonNegative(tx));
    }

    /*
    * (4) all of the transaction output values are non-negative
     */
    private boolean isTransactionOutputsAllNonNegative(Transaction tx) {
        boolean isValid = false;
        for (Transaction.Output txOutput : tx.getOutputs()) {
            if (txOutput.value < 0){
                return false;
            }
            else {
                isValid = true;
            }
        }
        return isValid;
    }
    /*
    * (3) No UTXO is claimed multiple times by a Transaction
     */
    private boolean isUTXOClaimedMultipleTimes(Transaction tx) {
        ArrayList<UTXO> allUTXOs = utxoPool.getAllUTXO();
        for (int i = 0; i <= allUTXOs.size(); i++) {
            UTXO utxo = allUTXOs.get(i);
            for (int j = i + 1; j < allUTXOs.size(); j++) {
                if (utxo.equals(allUTXOs.get(j))){
                    return true;
                }
            }
        }
        return false;
    }
    /*
    * (2) The signatures on each input of the transaction are valid
     */
    private boolean checkSignatures(Transaction tx) {
        boolean isValid = false;
        for (Transaction.Input txInput : tx.getInputs()) {
            byte[] signature = txInput.signature;
            UTXO utxo = new UTXO(txInput.prevTxHash, txInput.outputIndex);
            if (utxoPool.contains(utxo)) {
                PublicKey publicKey = utxoPool.getTxOutput(utxo).address;
                byte[] message = tx.getRawDataToSign(txInput.outputIndex);
                isValid = Crypto.verifySignature(publicKey, message, signature);
                if (!isValid) {
                    break;
                } else {
                    isValid = true;
                }
            }
            else {
                break;
            }
        }
        return isValid;
    }

    /*
    * (1) All outputs in the Transaciton are in the current UTXO pool
    */
    private boolean checkOutputs(Transaction tx) {
        boolean check = false;
        //check all outputs
        for (Transaction.Output txOutput : tx.getOutputs()) {
            ArrayList<UTXO> allUTXOs = utxoPool.getAllUTXO();
            UTXO found = allUTXOs.stream().filter(utxo -> {
                Transaction.Output poolTxOutput = utxoPool.getTxOutput(utxo);
                if ((txOutput.address.equals(poolTxOutput.address)) &&
                        (txOutput.value == (poolTxOutput.value)))
                    return true;
                else
                    return false;
            }).findAny().orElse(null);
            if (found != null) {
                check = true;
                continue;
            } else {
                check = false;
                break;
            }
        }
        return check;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        return null;
    }

}
