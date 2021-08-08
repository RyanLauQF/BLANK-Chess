import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

public class AI {
    protected final boolean isWhite;
    protected final Board board;
    private final OpeningTrie openingBook;  // opening book used by AI
    public final TranspositionTable TT;
    private boolean isUsingOpeningBook;
    private int moveNum;
    private boolean isDoingNullMove;

    private static final int MAX_PLY = 60;
    private static final int REDUCTION_CONSTANT = 2;

    public int ply;
    private int maxPly;
    private int nodeCount;
    private int cutOffCount;

    public short[][] killerMoves;
    public short[][] historyMoves;

    // A clock to limit the search duration
    private final Clock timer;
    private boolean searchStoppedByClock;

    // Used to obtain Principle Variation from iterative deepening search
    private short[][] PVMoves;
    private int[] PVLength;

    public AI(boolean isWhite, Board board) throws IOException {
        this.isWhite = isWhite;
        this.board = board;
        this.openingBook = new OpeningTrie(isWhite);    // builds the opening book for the AI
        this.TT = new TranspositionTable();
        this.moveNum = 0;  // number of moves made by this AI
        this.isUsingOpeningBook = true;
        this.timer = new Clock();
        this.killerMoves = new short[2][MAX_PLY];
        this.historyMoves = new short[64][64];
        this.PVMoves = new short[MAX_PLY][MAX_PLY];
        this.PVLength = new int[MAX_PLY];
        this.isDoingNullMove = false;
    }

    public boolean isWhite(){
        return isWhite;
    }

    // selects a random move when it is the AI turn
    public short getMove(){
        ArrayList<Short> moves = board.getAllLegalMoves();
        Random rand = new Random();
        int randomMove = rand.nextInt(moves.size());
        System.out.println("Making Random Move!");
        return moves.get(randomMove);
    }

    public short getOpeningMove(){
        Move previousMove = board.getPreviousMove();
        short openingMove = -1; // return -1 if the opening move is not found

        if(previousMove == null){   // first move being made. AI opening book does not need to record opponent move
            openingMove = getMoveFromOpeningBook();
            System.out.println("Using Opening Book!");
            System.out.println("bestmove " + MoveGenerator.toString(openingMove));
            return openingMove;
        }
        else{
            short previousMoveMade = previousMove.getEncodedMove();
            boolean moveExistsInBook = false;
            String lastMove = null;
            // check if the previous move made by opponent exists in opening book
            previousMove.unMake();  // undo previous move and check
            for(String bookMoves : openingBook.getSetOfBookMoves()){
                if(PGNExtract.convertNotationToMove(board, !isWhite(), bookMoves) == previousMoveMade){
                    moveExistsInBook = true;
                    lastMove = bookMoves;   // get the PGN String format of the move
                    break;
                }
            }
            previousMove.makeMove();  // make the move again
            if(moveExistsInBook){
                // update the opening book with previous opponent move and get a response
                openingBook.makeMove(lastMove);
                openingMove = getMoveFromOpeningBook();
                System.out.println("Using Opening Book!");
                System.out.println("bestmove " + MoveGenerator.toString(openingMove));
                return openingMove;
            }
            else{
                isUsingOpeningBook = false;
            }
        }
        return openingMove;
    }

    public short getMoveFromOpeningBook(){
        // convert the move from algebraic notation to an encoded move.
        String PGN_notation = openingBook.getNextMove();
        openingBook.makeMove(PGN_notation); // make the AI's move in opening book
        return PGNExtract.convertNotationToMove(board, isWhite(), PGN_notation);
    }

    public int searchBestMove(int depth, int searchPly, int alpha, int beta){
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

        // check for move repetition
        if(searchPly != 0 && board.repetitionHistory.containsKey(zobrist) && (board.repetitionHistory.get(zobrist) >= 1)){
            // do not play any same position more than once
            return 0;
        }

        // evaluate the positions after doing a quiescence search to remove horizon effect (search all captures)
        if(depth == 0){
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
            int score = -searchBestMove(depth - 1 - REDUCTION_CONSTANT, searchPly + 1, -beta, -beta + 1);   // set R to 2
            nullMove.unmakeNullMove();
            isDoingNullMove = false;

            // time is up
            if (timer.isTimeUp()) {
                searchStoppedByClock = true;
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

        for (Short encodedMove : MoveOrdering.orderMoves(encodedMoves, board, TT, this)) {
            Move move = new Move(board, encodedMove);
            move.makeMove();
            //            // PVS Search
//            if (searched_moves == 0) {
//                score = -alpha_beta(pos, -beta, -alpha, depth - 1, search_info, true);
//            }
//            else {
//                // Late move reductions
//                if ((searched_moves >= 4) & (!in_check) & (!move_list.moves[i].is_capture()) & (depth > 2))
//                    score = -alpha_beta(pos, -alpha-1, -alpha, depth - 2, search_info, true);
//                else
//                    score = -alpha_beta(pos, -alpha-1, -alpha, depth - 1, search_info, true);
//
//                if (score > alpha)
//                    score = -alpha_beta(pos, -beta, -alpha, depth - 1, search_info, true);
//            }

            int searchedScore = -searchBestMove(depth - 1, searchPly + 1, -beta, -alpha);
            move.unMake();

            // time is up
            if (timer.isTimeUp()) {
                searchStoppedByClock = true;
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

    /**
     * Evaluates the current position on the board by continuing to search all possible capture lines to reduce horizon effect
     * i.e. Prevents the AI from blundering a piece due to search being cut at a certain depth causing it to not "see" opponent attacks
     */
    private int quiescenceSearch(int alpha, int beta) {
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
        for (Short encodedMove : MoveOrdering.orderMoves(captureMoves, board, TT, this)) {
            Move move = new Move(board, encodedMove);

            ply++;
            maxPly = Math.max(maxPly, ply);
            move.makeMove();
            int searchedScore = -quiescenceSearch(-beta, -alpha);
            move.unMake();
            ply--;

            // time is up
            if (timer.isTimeUp()) {
                searchStoppedByClock = true;
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

    public short iterativeDS(double searchDuration, boolean enableOpeningBook){
        moveNum++;
        short bestMove = 0;

        // gets opening moves from opening book for the first few moves (up to 8)
        if(this.moveNum <= 8 && isUsingOpeningBook && enableOpeningBook){
            bestMove = getOpeningMove();

            // checks if the opening book contains the move, if it does not, -1 is returned
            if(bestMove != -1){
                return bestMove;
            }
        }

        System.out.println("Time Allocated: " + searchDuration + " seconds");

        // set the time for search.
        timer.setTime(searchDuration);
        timer.start();  // start the clock
        searchStoppedByClock = false;

        short currentMove;
        double iterationEndTime, timeElapsedSinceStart;

        // iterative deepening search
        for(int curr_depth = 1; curr_depth <= MAX_PLY; curr_depth++){
            long start = System.currentTimeMillis();

            // reset all counters for each iteration
            resetSearch();

            int score = searchBestMove(curr_depth, 0, Integer.MIN_VALUE + 1, Integer.MAX_VALUE);

            long finish = System.currentTimeMillis();
            long timeElapsed = finish - start;
            float convertTime = (float) timeElapsed / 1000;

            // best move found in current iteration
            currentMove = PVMoves[0][0];

            // end time of this iteration
            iterationEndTime = System.currentTimeMillis();

            // time taken to get to this iteration (to determine if we should continue to search next iteration in given time)
            timeElapsedSinceStart = iterationEndTime - timer.getStartTime();

            // if the current search was not stopped by clock, use the results of the search
            if(!searchStoppedByClock){
                // set best move to the best move of current iteration
                bestMove = currentMove;

                // information obtained from the search
                String searchInfo = "info depth " + curr_depth + " maxDepth " + maxPly + " score cp " + score
                        + " nodes " + nodeCount + " tbCut " + cutOffCount + " time " + convertTime;

                // PV line obtained from the search
                StringBuilder PVLine = new StringBuilder(" pv ");
                for(int i = 0; i < PVLength[0]; i++){
                    PVLine.append(MoveGenerator.toString(PVMoves[0][i]));
                    PVLine.append(" ");
                }
                System.out.println(searchInfo + PVLine.toString());
            }

            // hard stop the search if:
            //      - timer is up
            //      - remaining time < time taken to get to current ply

            if(timer.isTimeUp() || (timer.getRemainingTime() < timeElapsedSinceStart)){
                System.out.println("bestmove " + MoveGenerator.toString(bestMove));
                System.out.println("Time Taken: " + timeElapsedSinceStart);
                break;
            }
        }
        System.out.println("Transposition Table Size: " + TT.table.size());
        return bestMove;
    }

    /**
     * If there are less than or equal to 7 pieces left, end game is reached
     * @return true if the AI side is now in end game phase
     */
    public boolean isEndGame(){
        return board.getPieceList(isWhite()).getCount() <= 7 && board.getPieceList(!isWhite()).getCount() <= 7;
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
     * Unit Testing
     */
    public static void main(String[] args) throws IOException {
        Board board = new Board();
        board.init(FENUtilities.trickyFEN);
        AI testAI = new AI(false, board);

        int timePerSearch = 15;
        testAI.iterativeDS(timePerSearch, false);
    }
}
