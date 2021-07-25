import java.util.HashMap;

public class TranspositionTable {
    public static class TTEntry {
        public long zobristHash;
        public short bestMove;
        public int eval;
        public byte depth;

        public TTEntry(long zobristHash, short bestMove, byte depth, int eval){
            this.zobristHash = zobristHash;
            this.bestMove = bestMove;
            this.eval = eval;
            this.depth = depth;
        }
    }

    public final HashMap<Long, TTEntry> table;

    public TranspositionTable(){
        table = new HashMap<>();
    }

    public void store(long zobristHash, short bestMove, byte depth, int eval){
        TTEntry entry = new TTEntry(zobristHash, bestMove, depth, eval);
        table.put(zobristHash, entry);
    }

    public boolean containsKey(long zobristHash){
        return table.containsKey(zobristHash);
    }

    public TTEntry getEntry(long zobristHash){
        return table.get(zobristHash);
    }
}
