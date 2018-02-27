import java.security.PublicKey;
import java.util.ArrayList;
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
                    if (utxoPool.getTxOutput(utxo) != null) {
                        Transaction.Output tOutput = utxoPool.getTxOutput(utxo);
                        if (tOutput.address != null) {
                            PublicKey publicKey = tOutput.address;
                            if (tx.getRawDataToSign(i) != null) {
                                byte[] message = tx.getRawDataToSign(i);
                                byte[] signature = txInput.signature;
                                if (publicKey != null && message != null && signature != null && message.length > 0
                                        && signature.length > 0) {
                                    isValid = Crypto.verifySignature(publicKey, message, signature);
                                    if (!isValid) {
                                        return false;
                                    }
                                } else {
                                    return false;
                                }
                            } else {
                                return false;
                            }
                        } else {
                            return false;
                        }
                    } else {
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
    /*
     * handleTxs should also update its internal UTXOPool to reflect the current set of unspent
     * transaction outputs, so that future calls to handleTxs() and isValidTx() are able to
     * correctly process/validate transactions that claim outputs from transactions that were
     * accepted in a previous call to  handleTxs() .
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        ArrayList<Transaction> validTransactions = new ArrayList<>();
        ArrayList<Transaction> reTryTransactions = new ArrayList<>();
        Set<UTXO> utxoSet = new HashSet<>();
        for (int i = 0; i < possibleTxs.length; i++) {
            if (isValidTx(possibleTxs[i])) {
                Transaction processTransaction = possibleTxs[i];
                if (!checkDoubleSpend(utxoSet, processTransaction)) {
                    updatePool(processTransaction);
                    validTransactions.add(processTransaction);
                }
            }
            //retry later as they are unordered transactions, and some transactions might depend
            //on others.
            else {
                reTryTransactions.add(possibleTxs[i]);
            }
        }
        processFailedTransactions(true, reTryTransactions, validTransactions);

        return validTransactions.toArray(new Transaction[validTransactions.size()]);
    }

    private boolean checkDoubleSpend(Set<UTXO> utxoSet, Transaction transaction) {

        for (Transaction.Input txInput : transaction.getInputs()) {
            UTXO utxo = new UTXO(txInput.prevTxHash, txInput.outputIndex);
            if (!utxoSet.contains(utxo)) {
                utxoSet.add(utxo);
            }
            else {
                return true; // double spend if there is a utxo already in the set
            }
        }
        return false;
    }

    private ArrayList<Transaction> processFailedTransactions(boolean retry, ArrayList<Transaction> failedTransactions,
                                                             ArrayList<Transaction> validTransactions) {

        ArrayList<Transaction> retryTransactions = new ArrayList<>();
        boolean poolUpdated = false;

        if (!retry) {
            return validTransactions;
        }
        else {
            for (Transaction retryTransaction : failedTransactions) {
                if (isValidTx(retryTransaction)) {
                    updatePool(retryTransaction);
                    validTransactions.add(retryTransaction);
                    poolUpdated = true;
                }
                else {
                    retryTransactions.add(retryTransaction);
                }
            }
            if (poolUpdated) {
                processFailedTransactions(true, failedTransactions, validTransactions);
            }
            else {
                processFailedTransactions(false, failedTransactions, validTransactions);
            }
        }
        return validTransactions;
    }

    private void updatePool(Transaction validTransaction) {

        ArrayList<Transaction.Input> inputs = validTransaction.getInputs();
        
        for (Transaction.Input txInput : inputs){
            UTXO removeUtxo = new UTXO(txInput.prevTxHash, txInput.outputIndex); // passes test 15
            utxoPool.removeUTXO(removeUtxo);
        }
        for (int i = 0; i < validTransaction.getOutputs().size(); i++) {
            UTXO utxo = new UTXO(validTransaction.getHash(), i);
            utxoPool.addUTXO(utxo, validTransaction.getOutput(i));
        }
    }


}
