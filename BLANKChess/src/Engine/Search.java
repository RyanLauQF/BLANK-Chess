import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class Search {

    // Used to stop search when "stop" command is given
    private final BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    private boolean searchStopped;

    // Late Move Reduction - reduction factor pre-calculated look-up table
    private static final int MAX_PLY = 60;
    private static final int MAX_MOVES = 300;
    private static final int REDUCTION_LIMIT = 3;
    private static final int[][] REDUCTION_TABLE;

    // Search info
    public int ply;
    private int maxPly;
    private int nodeCount;
    private int cutOffCount;

    // Memoization of searched nodes
    public TranspositionTable TT;
    public short[][] killerMoves;
    public short[][] historyMoves;

    // null move pruning
    private static final int REDUCTION_CONSTANT = 2;

    // Used to check if the current search iteration is following a null move line
    private boolean isDoingNullMove;

    // A clock to limit the search duration
    private final Clock timer;

    // Used to obtain Principle Variation from iterative deepening search
    private short[][] PVMoves;
    private int[] PVLength;

    // Root board state where search begins
    public Board board;

    /*
     * init at the start of the program to fill up reduction factors for varying depth and move numbers
     */
    static {
        REDUCTION_TABLE = new int[MAX_PLY][MAX_MOVES];
        for(int depth = 0; depth < MAX_PLY; depth++){
            for(int moveCount = 0; moveCount < MAX_MOVES; moveCount++){
                REDUCTION_TABLE[depth][moveCount] = calculateReduction(depth, moveCount);
            }
        }

        for(int depth = 0; depth < MAX_PLY; depth++){
            REDUCTION_TABLE[depth][0] = 0;
            REDUCTION_TABLE[depth][1] = 0;
        }

        for(int moveCount = 0; moveCount < MAX_MOVES; moveCount++){
            REDUCTION_TABLE[0][moveCount] = 0;
            REDUCTION_TABLE[1][moveCount] = 0;
        }
    }

    /**
     * Constructor
     * @param board refers to the root board state where the search will begin
     */
    public Search(Board board){
        this.board = board;
        this.isDoingNullMove = false;
        this.timer = new Clock();

        this.TT = new TranspositionTable();
        this.killerMoves = new short[2][MAX_PLY];
        this.historyMoves = new short[64][64];

        this.PVMoves = new short[MAX_PLY][MAX_PLY];
        this.PVLength = new int[MAX_PLY];
    }


    /**
     * search for best move using iterative deepening for a given period of time or until "stop" command is given
     * @param searchDuration refers to the time allocated for engine to search the current position
     * @return the encoded move determined to be the best move
     */
    public short startSearch(double searchDuration){

        // creates a thread to listen for "stop" command in the background
        System.out.println("Time Allocated: " + searchDuration + " seconds");

        // set the time for search.
        timer.setTime(searchDuration);
        timer.start();  // start the clock

        double iterationEndTime, timeElapsedSinceStart;
        short currentMove, bestMove = 0;
        int totalNodeCount = 0;

        searchStopped = false;

        // iterative deepening search
        for (int curr_depth = 1; curr_depth <= MAX_PLY; curr_depth++) {

            // reset all variables for each iteration
            resetSearch();

            // search for bestmove for current iteration
            int score = negamax(curr_depth, 0, Integer.MIN_VALUE + 1, Integer.MAX_VALUE);

            // best move found in current iteration
            currentMove = PVMoves[0][0];

            // end time of this iteration
            iterationEndTime = System.currentTimeMillis();

            // time taken to get to this iteration (to determine if we should continue to search next iteration in given time)
            timeElapsedSinceStart = iterationEndTime - timer.getStartTime();

            // if the current search was not stopped by clock, use the results of the search
            if (!searchStopped) {
                // set best move to the best move of current iteration
                bestMove = currentMove;

                // information obtained from the search
                String searchInfo = "info depth " + curr_depth +
                        " seldepth " + maxPly +
                        " score cp " + score +
                        " nodes " + (totalNodeCount += nodeCount) +
                        " ttCut " + cutOffCount +
                        " time " + (int) timeElapsedSinceStart;

                // PV line obtained from the search
                StringBuilder PVLine = new StringBuilder(" pv ");
                for (int i = 0; i < PVLength[0]; i++) {
                    PVLine.append(MoveGenerator.toString(PVMoves[0][i]));
                    PVLine.append(" ");
                }
                System.out.println(searchInfo + PVLine.toString());
            }

            /*
             * hard stop the search if:
             *      - timer is up
             *      - remaining time < time taken to get to current ply
             *      - "stop" command is given
             */

            if (timer.isTimeUp() || (timer.getRemainingTime() < timeElapsedSinceStart) || searchStopped) {
                System.out.println("bestmove " + MoveGenerator.toString(bestMove));
                System.out.println("Time Taken: " + timeElapsedSinceStart);
                break;
            }
        }
        System.out.println("Transposition Table Size: " + TT.table.size());

        searchStopped = false;

        return bestMove;
    }

    public void listen() throws IOException {
        String s;
        if(br.ready()){
            s = br.readLine();
            if(s.equals("stop")){
                stopSearch();
            }
            else if(s.equals("quit")){
                br.close();
                System.exit(0);
            }
        }
    }

    public void stopSearch(){
        searchStopped = true;
        System.out.println("stopping...");
    }

    public int negamax(int depth, int searchPly, int alpha, int beta){
        PVLength[searchPly] = searchPly;

        long zobrist = board.getZobristHash();
        // obtain transposition table data if current position has already been evaluated before
        if(TT.containsKey(zobrist)){
            TranspositionTable.TTEntry entry = TT.getEntry(zobrist);

            // if the entry depth is greater than current depth, use the stored evaluation as it is more accurate due to deeper search
            if(entry.depth >= depth && searchPly != 0){
                if(entry.entry_TYPE == TranspositionTable.EXACT_TYPE){
                    cutOffCount++;
                    return entry.eval;
                }
                else if(entry.entry_TYPE == TranspositionTable.LOWERBOUND_TYPE){
                    alpha = Math.max(alpha, entry.eval);
                }
                else if(entry.entry_TYPE == TranspositionTable.UPPERBOUND_TYPE){
                    beta = Math.min(beta, entry.eval);
                }
                if(alpha >= beta){
                    cutOffCount++;
                    return entry.eval;
                }
            }
        }

        // every 2047 nodes, check for UCI commands
        if((nodeCount & 2047) == 0){
            try{
                listen();
            }
            catch (IOException ignored){
                System.out.println("Unable to read input!");
            }
        }

        // check for move repetition
        if(searchPly != 0 && board.repetitionHistory.containsKey(zobrist) && (board.repetitionHistory.get(zobrist) >= 1)){
            // do not play any same position more than once
            return 0;
        }

        // evaluate the positions after doing a quiescence search to remove horizon effect (search all captures)
        if(depth <= 0){
            ply = searchPly;
            return quiescenceSearch(alpha, beta);
        }
        nodeCount++;

        // null move pruning
        boolean isKingChecked = board.isKingChecked();
        if(depth >= 3 && !isDoingNullMove && !isKingChecked && !isEndGame()){
            Move nullMove = new Move(board, (short) 0);
            isDoingNullMove = true;
            nullMove.makeNullMove();
            int score = -negamax(depth - 1 - REDUCTION_CONSTANT, searchPly + 1, -beta, -beta + 1);   // set R to 2
            nullMove.unmakeNullMove();
            isDoingNullMove = false;

            // time is up
            if (timer.isTimeUp() || searchStopped) {
                searchStopped = true;
                return 0;
            }

            if (score >= beta){
                return beta;
            }
        }

        // Check extension
        if(isKingChecked){
            depth++;
        }

        ArrayList<Short> encodedMoves = board.getAllLegalMoves();

        // Game has ended
        if(encodedMoves.size() == 0){
            // checkmate found
            if(isKingChecked){
                return -EvalUtilities.CHECKMATE_SCORE + searchPly;
            }

            // draw
            return 0;
        }

        short bestMove = 0;
        byte moveFlag = TranspositionTable.UPPERBOUND_TYPE;
        int bestScore = Integer.MIN_VALUE + 1;
        int moveCount = 0;
        int searchedScore;

        for (Short encodedMove : MoveOrdering.orderMoves(encodedMoves, this)) {
            moveCount++;
            Move move = new Move(board, encodedMove);
            move.makeMove();

            // pv move, do a full search
            if(moveCount == 1){
                searchedScore = -negamax(depth - 1, searchPly + 1, -beta, -alpha);
            }
            else{
                // late move reductions
                if(depth >= REDUCTION_LIMIT && moveCount > 1
                        && !MoveGenerator.isPromotion(encodedMove)
                        && !MoveGenerator.isCapture(encodedMove)
                        && !isKingChecked){

                    // do reduced search based on reduction factor with a narrowed window
                    int reduction = REDUCTION_TABLE[depth][moveCount];
                    searchedScore = -negamax(depth - 1 - reduction, searchPly + 1, -alpha - 1, -alpha);
                }
                else{
                    // do a full-depth search
                    searchedScore = alpha + 1; // a trick to ensure full-depth search is continued
                }

                // PVS search
                if(searchedScore > alpha){
                    searchedScore = -negamax(depth - 1, searchPly + 1, -alpha - 1, -alpha);

                    // re-search the move
                    if(searchedScore > alpha && searchedScore < beta){
                        searchedScore = -negamax(depth - 1, searchPly + 1, -beta, -alpha);
                    }
                }
            }

            move.unMake();

            // time is up
            if (timer.isTimeUp() || searchStopped) {
                searchStopped = true;
                return 0;
            }

            if(searchedScore >= bestScore){
                bestScore = searchedScore;
                bestMove = encodedMove;
            }

            if(bestScore > alpha){
                alpha = bestScore;
                moveFlag = TranspositionTable.EXACT_TYPE;

                // write PV move
                PVMoves[searchPly][searchPly] = encodedMove;
                // copy move from deeper ply into a current ply's line
                if (PVLength[searchPly + 1] - searchPly + 1 >= 0){
                    System.arraycopy(PVMoves[searchPly + 1], searchPly + 1, PVMoves[searchPly], searchPly + 1, PVLength[searchPly + 1] - searchPly + 1);
                }
                PVLength[searchPly] = PVLength[searchPly + 1];
            }

            // cut off has occurred
            if(alpha >= beta) {
                // store in tranposition table
                TT.store(board.getZobristHash(), encodedMove, (byte) depth, alpha, TranspositionTable.LOWERBOUND_TYPE);

                // if the move that causes a cut off is a quiet move (not a capture) store move as killer and history moves
                if(!MoveGenerator.isCapture(encodedMove)){
                    // store killer move
                    killerMoves[1][searchPly] = killerMoves[0][searchPly];
                    killerMoves[0][searchPly] = encodedMove;

                    // store history move data
                    historyMoves[MoveGenerator.getStart(encodedMove)][MoveGenerator.getEnd(encodedMove)]++;
                }

                return alpha;
            }
        }

        // store the best move at current position
        TT.store(board.getZobristHash(), bestMove, (byte) depth, bestScore, moveFlag);

        return bestScore;
    }

    /**11563
     * Evaluates the current position on the board by continuing to search all possible capture lines to reduce horizon effect
     * i.e. Prevents the AI from blundering a piece due to search being cut at a certain depth causing it to not "see" opponent attacks
     */
    private int quiescenceSearch(int alpha, int beta) {
        // every 2047 nodes, check for UCI commands
        if((nodeCount & 2047) == 0){
            try{
                listen();
            }
            catch (IOException ignored){
                System.out.println("Unable to read input!");
            }
        }

        int stand_pat = EvalUtilities.evaluate(board);
        if(stand_pat >= beta){
            return stand_pat; // fail soft
        }

        // Delta pruning
        int BIG_DELTA = Queen.QUEEN_VALUE; // queen value
        if (stand_pat < (alpha - BIG_DELTA)) {
            return stand_pat;
        }
        nodeCount++;

        if(alpha < stand_pat){
            alpha = stand_pat;
        }

        ArrayList<Short> captureMoves = board.getAllCaptures();
        for (Short encodedMove : MoveOrdering.orderMoves(captureMoves, this)) {
            Move move = new Move(board, encodedMove);

            ply++;
            maxPly = Math.max(maxPly, ply);
            move.makeMove();
            int searchedScore = -quiescenceSearch(-beta, -alpha);
            move.unMake();
            ply--;

            // time is up
            if (timer.isTimeUp() || searchStopped) {
                searchStopped = true;
                return 0;
            }

            // cut-off has occurred
            if(searchedScore >= beta) {
                return beta;
            }
            if(searchedScore > alpha) {
                alpha = searchedScore;
            }
        }
        return alpha;
    }

    /**
     * Used to reset all counters / tables to prepare for next search
     */
    private void resetSearch(){
        ply = 0;
        maxPly = 0;
        nodeCount = 0;
        cutOffCount = 0;
        killerMoves = new short[2][MAX_PLY];
        historyMoves = new short[64][64];
        PVMoves = new short[MAX_PLY][MAX_PLY];
        PVLength = new int[MAX_PLY];
        isDoingNullMove = false;
    }

    /**
     * If there are less than or equal to 7 pieces left, end game is reached
     * @return true if the AI side is now in end game phase
     */
    private boolean isEndGame(){
        return board.getPieceList(true).getCount() <= 7 && board.getPieceList(false).getCount() <= 7;
    }

    private static int calculateReduction(int depth, int moveCount){
        return (int) ((1 / 1.95) * Math.log(depth) * Math.log(moveCount));
    }

    /**
     * Unit Testing
     */
    public static void main(String[] args) {
        Board board = new Board();
        board.init(FENUtilities.trickyFEN);

        Search search = new Search(board);
        search.startSearch(20);
    }
}
