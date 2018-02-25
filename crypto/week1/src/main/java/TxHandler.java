import java.security.PublicKey;
import java.util.HashSet;
import java.util.Set;

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
                isTransactionOutputsAllNonNegative(tx) &&
                isSumOfInputsValid(tx));
    }

    /*
    * (5) The sum of a transactions inputs >= sum of its output values
     */
    private boolean isSumOfInputsValid(Transaction tx) {
        double inputTotal = 0;
        double outputTotal = 0;
        for (Transaction.Input txInput : tx.getInputs()) {
            UTXO utxo = new UTXO(txInput.prevTxHash, txInput.outputIndex);
            if (utxoPool.contains(utxo)) {
                double inputValue = utxoPool.getTxOutput(utxo).value;
                inputTotal += inputValue;
            } else {
                return false;
            }
        }
        for (Transaction.Output txOutput : tx.getOutputs()) {
            if (txOutput.value < 0) {
                return false;
            }
            else {
                double outputValue = txOutput.value;
                outputTotal += outputValue;
            }
        }
        return (inputTotal >= outputTotal);

    }

    /*
    * (4) all of the transaction output values are non-negative
     */
    private boolean isTransactionOutputsAllNonNegative(Transaction tx) {
        for (Transaction.Output txOutput : tx.getOutputs()) {
            if (txOutput.value < 0) {
                return false;
            }
        }
        return true;
    }
    /*
    * (3) No UTXO is claimed multiple times by a Transaction
     */
    private boolean isUTXOClaimedMultipleTimes(Transaction tx) {
        Set<UTXO> utxoSet = new HashSet<>();
        for (Transaction.Input txInput : tx.getInputs()) {
            UTXO utxo = new UTXO(txInput.prevTxHash, txInput.outputIndex);
            if (!utxoSet.contains(utxo)) {
                utxoSet.add(utxo);
            }
            else {
                return true; // double spend if there is a utxo already in the set
            }
        }
        return false; // no double spend
    }
    /*
    * (2) The signatures on each input of the transaction are valid
     */
    private boolean checkSignatures(Transaction tx) {
        boolean isValid = false;
        int i = 0;
        for (Transaction.Input txInput : tx.getInputs()) {
            if (txInput.outputIndex >= 0 && txInput.prevTxHash.length > 0) {
                UTXO utxo = new UTXO(txInput.prevTxHash, txInput.outputIndex);
                if (utxoPool.contains(utxo)) {

                    PublicKey publicKey = utxoPool.getTxOutput(utxo).address;
                    byte[] message = tx.getRawDataToSign(i);
                    byte[] signature = txInput.signature;
                    if (publicKey != null && message != null && signature != null && message.length > 0
                            && signature.length > 0) {
                        isValid = Crypto.verifySignature(publicKey, message, signature);
                        if (!isValid) {
                            return false;
                        }
                    }
                    else {
                        return false;
                    }
                } else {
                    return false;
                }
                i++;
            } else {
                return false;
            }
        }
        return true;
    }

    /*
     * (1) All outputs in the Transaction are in the current UTXO pool
     */
   private boolean checkOutputs(Transaction tx) {

        //iterate through all inputs
        for (Transaction.Input txInput : tx.getInputs()) {
            UTXO utxoTx = new UTXO(txInput.prevTxHash, txInput.outputIndex);
            if (!utxoPool.contains(utxoTx)) {
                return false;
            }
        }
        return true;
    }
    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        return possibleTxs;
    }

}
