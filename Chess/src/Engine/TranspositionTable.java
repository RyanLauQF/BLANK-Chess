import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

public class TranspositionTable {
    public static byte EXACT_TYPE = 0;
    public static byte LOWERBOUND_TYPE = 1;
    public static byte UPPERBOUND_TYPE = 2;

    public static class TTEntry {
        public long zobristHash;
        public short bestMove;
        public int eval;
        public byte depth;
        public byte entry_TYPE;

        /*
         * Entry type refers the type of evaluation being stored:
         *
         *      0 - EXACT Evaluation, when we receive a definite evaluation
         *
         *      1 - LOWER BOUND evaluation: A move was found during the search that was too good, meaning the opponent will play a different move earlier on.
         *
         *      2 - UPPER BOUND Evaluation: (i.e eval was <= alpha for all moves in the position).
         *                                  No moves were better than what current player already has
         */
        public TTEntry(long zobristHash, short bestMove, byte depth, int eval, byte entry_TYPE){
            this.zobristHash = zobristHash;
            this.bestMove = bestMove;
            this.eval = eval;
            this.depth = depth;
            this.entry_TYPE = entry_TYPE;
        }
    }

    public final HashMap<Long, TTEntry> table;  // limit hashmap to 5 mill entries using queue to remove old hashes
    public final Queue<Long> zobristRemoval; // Remove the hashes added the transposition table on a FIFO basis

    public TranspositionTable(){    //8388608
        table = new HashMap<>(4194304); // inititate to a large enough size (2 ^ 22 used) to avoid resizing. Size must be in powers of 2;
        zobristRemoval = new LinkedList<>();
    }

    public void store(long zobristHash, short bestMove, byte depth, int eval, byte entry_TYPE){
        if(!table.containsKey(zobristHash)){
            zobristRemoval.add(zobristHash);
            if(zobristRemoval.size() >= 5000000){
                long hash = zobristRemoval.remove();
                table.remove(hash);
            }
        }

        TTEntry entry = new TTEntry(zobristHash, bestMove, depth, eval, entry_TYPE);
        table.put(zobristHash, entry);
    }

    public boolean containsKey(long zobristHash){
        return table.containsKey(zobristHash);
    }

    public TTEntry getEntry(long zobristHash){
        return table.get(zobristHash);
    }
}
