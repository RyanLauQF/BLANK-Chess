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

    public static int ALLOCATED_HASH_SIZE_MEGABYTES = 32; // default 32mb will be used
    public static int BYTES_PER_ENTRY = 32; // 32 bytes are used per entry

    public TTEntry[][] hashTable;
    public int HASH_ENTRY_SIZE;

    /**
     * Constructor
     *
     * Creates a two-leveled fixed-sized transposition table where the first level uses a depth-preferred scheme
     * while the second level uses an always replace scheme to handle index collisions in the hash table
     */
    public TranspositionTable(){
        // convert from megabytes to bytes
        int allocatedMemory = convertToBytes(ALLOCATED_HASH_SIZE_MEGABYTES);
        HASH_ENTRY_SIZE = allocatedMemory / BYTES_PER_ENTRY;

        // make sure that the hash entry size is an even number
        if(HASH_ENTRY_SIZE % 2 != 0){
            HASH_ENTRY_SIZE++;
        }

        // create the two-leveled transposition table
        hashTable = new TTEntry[HASH_ENTRY_SIZE / 2][2];
    }

    public void recordEntry(long zobristHash, short bestMove, byte depth, int eval, byte entry_TYPE){
        TTEntry entry = new TTEntry(zobristHash, bestMove, depth, eval, entry_TYPE);

        // obtain the hash key for the entry inside the hashtable
        int hashKey = Math.abs((int) (zobristHash % (HASH_ENTRY_SIZE / 2)));

        // new entry into transposition table
        if(hashTable[hashKey][0] == null){
            // fill up first level of transposition table before shifting entry to second level
            hashTable[hashKey][0] = entry;
        }

        // pre-existing entry is found at the same hashKey.
        else{
            // get first-level entry
            TTEntry existingEntry = hashTable[hashKey][0];

            // at first level, compare by depth preferred to replace entries
            if(existingEntry.depth <= depth){

                // since new entry is searched to a greater depth, shift first-level entry to the second level
                hashTable[hashKey][1] = hashTable[hashKey][0];

                // replace first-level entry with the new entry
                hashTable[hashKey][0] = entry;
            }
            else{
                // place new entry into second level with an "always" replace scheme
                // if second-level is not empty, replace second-level entry with new entry
                hashTable[hashKey][1] = entry;
            }
        }
    }

    public boolean containsKey(long zobristHash){
        int hashKey = Math.abs((int) (zobristHash % (HASH_ENTRY_SIZE / 2)));
        TTEntry[] entries = hashTable[hashKey];
        if(entries[0] != null && entries[0].zobristHash == zobristHash){
            return true;
        }
        else {
            return entries[1] != null && entries[1].zobristHash == zobristHash;
        }
    }

    public TTEntry getEntry(long zobristHash){
        TTEntry entry = null;
        int hashKey = Math.abs((int) (zobristHash % (HASH_ENTRY_SIZE / 2)));
        if(hashTable[hashKey][0].zobristHash == zobristHash){
            entry = hashTable[hashKey][0];
        }
        else if(hashTable[hashKey][1].zobristHash == zobristHash){
            entry = hashTable[hashKey][1];
        }
        return entry;
    }

    public int size(){
        int counter =0;
        for(int i = 0 ; i < HASH_ENTRY_SIZE / 2 ; i++){
            if(hashTable[i][0] != null){
                counter++;
            }
            if(hashTable[i][0] != null){
                counter++;
            }
        }
        return counter;
    }

    public static int convertToBytes(int MEGABYTES){
        return MEGABYTES * 1000000;
    }

//    Single-level depth-replacement Transposition table
//    ================================================================
//    public void recordEntry(long zobristHash, short bestMove, byte depth, int eval, byte entry_TYPE){
//        TTEntry entry = new TTEntry(zobristHash, bestMove, depth, eval, entry_TYPE);
//        // obtain the hash key for the entry inside the hashtable
//        int hashKey = Math.abs((int) (zobristHash % DEFAULT_ENTRY_SIZE));
//
//        if(hashTable[hashKey] == null){
//            // new entry into transposition table
//            hashTable[hashKey] = entry;
//        }
//        else{
//            // pre-existing entry is found at the same hashKey.
//            TTEntry existingEntry = hashTable[hashKey];
//
//            // compare by depth preferred to replace entries
//            if(existingEntry.depth <= depth){
//                hashTable[hashKey] = entry;
//            }
//        }
//    }
//
//    public boolean containsKey(long zobristHash){
//        int hashKey = Math.abs((int) (zobristHash % DEFAULT_ENTRY_SIZE));
//        TTEntry entry = hashTable[hashKey];
//        return entry != null && entry.zobristHash == zobristHash;
//    }
//
//    public TTEntry getEntry(long zobristHash){
//        int hashKey = Math.abs((int) (zobristHash % DEFAULT_ENTRY_SIZE));
//        return hashTable[hashKey];
//    }
//
//    public int size(){
//        int counter =0;
//        for(int i = 0 ; i < DEFAULT_ENTRY_SIZE ; i++){
//            if(hashTable[i] != null){
//                counter++;
//            }
//        }
//        return counter;
//    }

    /**
     * Unit Testing
     */
    public static void main(String[] args){
        Board board = new Board();
        board.init(FENUtilities.trickyFEN);

        Search search = new Search(board);
        search.startSearch(10);

//        Sample Entries:
//        ==================
//        -6356014439068973177 1812 4 1239 0
//        3542202792881453200 801 2 565 0
//        1582721375619406960 807 3 1202 0
//        2123288944664113115 1747 3 -1282 0
//        4803992617756591605 17675 1 -1089 0

//        for(long entry : search.TT.table.keySet()){
//            TranspositionTable.TTEntry e = search.TT.table.get(entry);
//            if(e.entry_TYPE == TranspositionTable.EXACT_TYPE){
//                System.out.println(e.zobristHash + " " + e.bestMove + " " + e.depth + " " + e.eval + " " + e.entry_TYPE);
//            }
//        }
    }
}
