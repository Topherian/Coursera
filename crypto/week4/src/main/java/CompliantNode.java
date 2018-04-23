import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

/* CompliantNode refers to a node that follows the rules (not malicious)*/
public class CompliantNode implements Node {

    private double p_graph;
    private double p_malicious;
    private double p_txDistribution;
    private double numRounds;
    private boolean[] followees;
    private static Set<Transaction> pendingTransactions = new HashSet<>();
    private static double currentRound = 0;
    private static double minRoundRejection = 3;
    private static Set<Integer> maliciousNodes = new HashSet<Integer>();
    private static Hashtable<Integer, Set<Transaction>> transactionHistory = new Hashtable<>();

    public CompliantNode(double p_graph, double p_malicious, double p_txDistribution, int numRounds) {
        this.p_graph = p_graph;
        this.p_malicious = p_malicious;
        this.p_txDistribution = p_txDistribution;
        this.numRounds = numRounds;
    }

    public void setFollowees(boolean[] followees) {
        this.followees = followees;
    }

    public void setPendingTransaction(Set<Transaction> pendingTransactions) {
        pendingTransactions.addAll(pendingTransactions);
    }

    public Set<Transaction> sendToFollowers() {
        return pendingTransactions;
    }

    /*
    * Malicious Notes could be:
    * Nodes not communicating at all
    * Nodes communicating only its own initial transactions
    * Nodes communicating transactions randomly
    * Nodes communicating only at the final round
    * Nodes communicating only at even or odd rounds
     */
    public void receiveFromFollowees(Set<Candidate> candidates) {
        for (Candidate candidate : candidates) {
            Integer nodeId = new Integer(candidate.sender);
            //not in the maliciousNodes list
            if (!maliciousNodes.contains(nodeId)) {
                Transaction transaction = candidate.tx;
                if (transaction != null) {
                    //new candidate node, but ignore new candidates in last round or minRoundRejection
                    //if (!transactionHistory.containsKey(nodeId) && (currentRound != numRounds)) {
                    if (!transactionHistory.containsKey(nodeId) && !((numRounds - currentRound) < minRoundRejection)){
                        transactionHistory.put(nodeId, new HashSet<>());
                        transactionHistory.get(nodeId).add(transaction);
                        if (!pendingTransactions.contains(transaction)) {
                            pendingTransactions.add(transaction);
                        }
                    }
                    if (transactionHistory.containsKey(nodeId)) {
                        if (!transactionHistory.get(nodeId).contains(transaction)) {
                            transactionHistory.get(nodeId).add(transaction);
                            if (!pendingTransactions.contains(transaction)) {
                                pendingTransactions.add(transaction);
                            }
                        }
                    }
                }
                // node is not communicating any transactions, could be malicious?
                else {
                    maliciousNodes.add(new Integer(candidate.sender));
                }
            }
        }
        //final round - remove any transactions from pending transactions where node only communicated
        //initial transactions or random transactions
        if (currentRound == numRounds) {
            for(Integer nodeId : transactionHistory.keySet()) {
                if (transactionHistory.get(nodeId).size() == 1) {
                    Set<Transaction> transactions = transactionHistory.get(nodeId);
                    for (Transaction transaction : transactions) {
                        pendingTransactions.remove(transaction);
                    }
                }
                // node was communicating random transactions - malicious?
                if (transactionHistory.get(nodeId).size() >= (numRounds - minRoundRejection)) {
                    Set<Transaction> transactions = transactionHistory.get(nodeId);
                    for (Transaction transaction : transactions) {
                        pendingTransactions.remove(transaction);
                    }
                }
            }
        }
        currentRound++;
    }
}
