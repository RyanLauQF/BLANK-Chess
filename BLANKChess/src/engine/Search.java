import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class Search {
    private static final int INFINITY = 120000;
    private static final int CHECKMATE_SCORE = 100000;
    private static final int CHECKMATE_THRESHOLD = 99000;
    private static final int DRAW_SCORE = 0;
    private static final int CONTEMPT_FACTOR = 20;
    private static final int STATIC_NULL_MOVE_PRUNING_MARGIN = 120;

    // Late Move Reduction - Reduction factor pre-calculated in look-up table
    private static final int MAX_PLY = 60;
    private static final int MAX_MOVES = 300;
    private static final int REDUCTION_LIMIT = 3;
    private static final int[][] REDUCTION_TABLE;

    // Futility Pruning Margins
    private static final int[] futilityMargin = {0, 200, 300, 500};

    // Null move pruning
    private static final int STANDARD_REDUCTION_CONSTANT = 2;
    private static final int DEEPER_REDUCTION_CONSTANT = 3;
    private boolean isDoingNullMove;

    // Used to stop search when "stop" command is given
    private final BufferedReader listener = new BufferedReader(new InputStreamReader(System.in));
    private boolean searchStopped;

    // Search info
    private int ply;
    private int maxPly;
    private int nodeCount;
    private int cutOffCount;

    // A clock to limit the search duration
    private final Clock timer;

    // Used to obtain Principle Variation from iterative deepening search
    public int[] PVLength;
    public short[][] PVMoves;
    public boolean followPVLine;
    public boolean pvMoveScoring;

    // Memoization of searched nodes with transposition table
    public TranspositionTable TT;
    public short[][] killerMoves;
    public short[][] historyMoves;

    // Root board state where search begins
    public Board board;

    /*
     * init at the start of the program to calculate reduction factors for varying depth and move numbers in move list
     */
    static {
        REDUCTION_TABLE = new int[MAX_PLY + 1][MAX_MOVES];
        for(int depth = 0; depth < MAX_PLY + 1; depth++){
            for(int moveCount = 0; moveCount < MAX_MOVES; moveCount++){
                REDUCTION_TABLE[depth][moveCount] = calculateReduction(depth, moveCount);
            }
        }

        for(int depth = 0; depth < MAX_PLY + 1; depth++){
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
     * @param TT refers to the transposition table used by the searcher
     */
    public Search(Board board, TranspositionTable TT){
        this.board = board;
        this.TT = TT;
        this.isDoingNullMove = false;
        this.timer = new Clock();
        this.killerMoves = new short[2][MAX_PLY];
        this.historyMoves = new short[64][64];
        this.PVMoves = new short[MAX_PLY][MAX_PLY];
        this.PVLength = new int[MAX_PLY];
    }

    /**
     * @param newBoard refers to the new board position to be searched
     */
    public void setBoard(Board newBoard){
        this.board = newBoard;
    }

    /**
     * search for best move using iterative deepening for a given period of time or until "stop" command is given
     * @param searchDuration refers to the time allocated for engine to search the current position
     * @return the encoded move determined to be the best move
     */
    public short startSearch(double searchDuration){
        System.out.println("Time Allocated: " + searchDuration + " seconds");

        // set the time for search.
        timer.setTime(searchDuration);
        timer.start();  // start the clock

        double iterationEndTime, timeElapsedSinceStart = 1;
        short currentMove, bestMove = 0;
        int totalNodeCount = 0;

        searchStopped = false;
        killerMoves = new short[2][MAX_PLY];
        historyMoves = new short[64][64];

        int numberOfMoves = board.getAllLegalMoves().size();

        // iterative deepening search
        for (int curr_depth = 1; curr_depth <= MAX_PLY; curr_depth++) {

            // reset all variables for each iteration
            resetSearch();

            // follow the Pv line found in previous search
            followPVLine = true;

            // search for bestmove for current iteration
            int score = negamax(curr_depth, 0, -INFINITY, INFINITY);

            // best move found in current iteration
            currentMove = PVMoves[0][0];

            // end time of this iteration
            iterationEndTime = System.currentTimeMillis();

            // time taken to get to this iteration (to determine if we should continue to search next iteration in given time)
            timeElapsedSinceStart = iterationEndTime - timer.getStartTime();
            if(timeElapsedSinceStart == 0){
                timeElapsedSinceStart = 1;  // prevent division by 0 when calculating nps
            }

            // if the current search was not stopped by clock, use the results of the search
            if (!searchStopped) {
                // set best move to the best move of current iteration
                bestMove = currentMove;

                String searchInfo;
                // information obtained from the search
                if(score > CHECKMATE_THRESHOLD || score < -CHECKMATE_THRESHOLD){
                    // print out mate value instead of score
                    String mate = "";
                    if(score > CHECKMATE_THRESHOLD){
                        int movesToMate = ((CHECKMATE_SCORE - score) / 2) + 1;
                        mate = String.valueOf(movesToMate);
                    }
                    if(score < -CHECKMATE_THRESHOLD){
                        int movesToMate = ((-CHECKMATE_SCORE - score) / 2) - 1;
                        mate = String.valueOf(movesToMate);
                    }
                    searchInfo = "info depth " + curr_depth +
                            " seldepth " + maxPly +
                            " score mate " + mate +
                            " nodes " + (totalNodeCount += nodeCount) +
                            " nps " + (long) ((totalNodeCount * 1000L) / timeElapsedSinceStart) +
                            " ttCut " + cutOffCount +
                            " time " + (int) timeElapsedSinceStart;
                }
                else{
                    searchInfo = "info depth " + curr_depth +
                            " seldepth " + maxPly +
                            " score cp " + score +
                            " nodes " + (totalNodeCount += nodeCount) +
                            " nps " + (long) ((totalNodeCount * 1000L) / timeElapsedSinceStart) +
                            " ttCut " + cutOffCount +
                            " time " + (int) timeElapsedSinceStart;
                }

                // PV line obtained from the search
                StringBuilder PVLine = new StringBuilder(" pv ");
                for (int i = 0; i < PVLength[0]; i++) {
                    PVLine.append(MoveGenerator.toString(PVMoves[0][i]));
                    PVLine.append(" ");
                }
                System.out.println(searchInfo + PVLine);
            }

            /*
             * hard stop the search if:
             *      - timer is up
             *      - remaining time < time taken to get to current ply
             *      - "stop" command is given
             *      - there is only 1 legal move to make
             */

            if (timer.isTimeUp()
                    || (timer.getRemainingTime() < timeElapsedSinceStart)
                    || searchStopped
                    || numberOfMoves == 1) {
                break;
            }
        }

        System.out.println("bestmove " + MoveGenerator.toString(bestMove));
        System.out.println("Time Taken: " + timeElapsedSinceStart);
        System.out.println("Transposition Table Size: " + TT.size());

        searchStopped = false;

        return bestMove;
    }

    /**
     * Listens for a "stop" or "quit" command during search
     */
    public void listen(){
        try{
            if(listener.ready()){
                String input = listener.readLine();
                if(input.equals("stop")){
                    stopSearch();
                }
                else if(input.equals("quit")){
                    listener.close();
                    System.exit(0);
                }
            }
        }
       catch (IOException ioException){
            System.out.println("Unable to read input!");
       }
    }

    /**
     * stops the ongoing search
     */
    public void stopSearch(){
        searchStopped = true;
        System.out.println("stopping...");
    }

    /**
     * search for best move found for a given depth
     * @param depth refers to the depth to search a position
     * @return the best move found
     */
    public short depthSearch(int depth){
        System.out.println("Target Depth: " + depth);

        double startTime = System.currentTimeMillis(), iterationEndTime, timeElapsedSinceStart = 1;
        short currentMove, bestMove = 0;
        int totalNodeCount = 0;
        searchStopped = false;

        killerMoves = new short[2][MAX_PLY];
        historyMoves = new short[64][64];

        // iterative deepening search
        for (int curr_depth = 1; curr_depth <= depth; curr_depth++) {

            // reset all variables for each iteration
            resetSearch();

            followPVLine = true;

            // search for bestmove for current iteration
            int score = negamax(curr_depth, 0, -INFINITY, INFINITY);

            // best move found in current iteration
            currentMove = PVMoves[0][0];

            // end time of this iteration
            iterationEndTime = System.currentTimeMillis();

            // time taken to get to this iteration (to determine if we should continue to search next iteration in given time)
            timeElapsedSinceStart = iterationEndTime - startTime;
            if(timeElapsedSinceStart == 0){
                timeElapsedSinceStart = 1;  // prevent division by 0 when calculating nps
            }

            // if the current search was not stopped by clock, use the results of the search
            if (!searchStopped) {
                // set best move to the best move of current iteration
                bestMove = currentMove;

                String searchInfo;
                // information obtained from the search
                if(score > CHECKMATE_THRESHOLD || score < -CHECKMATE_THRESHOLD){
                    // print out mate value instead of score
                    String mate = "";
                    if(score > CHECKMATE_THRESHOLD){
                        int movesToMate = ((CHECKMATE_SCORE - score) / 2) + 1;
                        mate = String.valueOf(movesToMate);
                    }
                    if(score < -CHECKMATE_THRESHOLD){
                        int movesToMate = ((-CHECKMATE_SCORE - score) / 2) - 1;
                        mate = String.valueOf(movesToMate);
                    }
                    searchInfo = "info depth " + curr_depth +
                            " seldepth " + maxPly +
                            " score mate " + mate +
                            " nodes " + (totalNodeCount += nodeCount) +
                            " nps " + (long) ((totalNodeCount * 1000L) / timeElapsedSinceStart) +
                            " ttCut " + cutOffCount +
                            " time " + (int) timeElapsedSinceStart;
                }
                else{
                    searchInfo = "info depth " + curr_depth +
                            " seldepth " + maxPly +
                            " score cp " + score +
                            " nodes " + (totalNodeCount += nodeCount) +
                            " nps " + (long) ((totalNodeCount * 1000L) / timeElapsedSinceStart) +
                            " ttCut " + cutOffCount +
                            " time " + (int) timeElapsedSinceStart;
                }

                // PV line obtained from the search
                StringBuilder PVLine = new StringBuilder(" pv ");
                for (int i = 0; i < PVLength[0]; i++) {
                    PVLine.append(MoveGenerator.toString(PVMoves[0][i]));
                    PVLine.append(" ");
                }

                System.out.println(searchInfo + PVLine);
            }
        }

        System.out.println("bestmove " + MoveGenerator.toString(bestMove));
        System.out.println("Time Taken: " + timeElapsedSinceStart);
        System.out.println("Transposition Table Size: " + TT.size());

        searchStopped = false;

        return bestMove;
    }

    public int negamax(int depth, int searchPly, int alpha, int beta){
        // ensure that the ply searched is not greater than max ply due to extensions
        if(searchPly > MAX_PLY - 1){
            return EvalUtilities.evaluate(board);
        }

        PVLength[searchPly] = searchPly;

        // check for following draw conditions:
        //      - fifty move rule
        //      - 3-move repetition (avoid playing any same position more than once)
        //      - insufficient material draw
        if(searchPly != 0 && isDraw(board)){
            // avoid taking draws unless down by more than contempt factor
            return CONTEMPT_FACTOR;
        }

        boolean isPV = (beta - alpha) > 1;

        // Probe transposition table if current position has already been evaluated before
        long zobrist = board.getZobristHash();
        if(searchPly != 0 && !isPV && TT.containsKey(zobrist)){
            TranspositionTable.TTEntry entry = TT.getEntry(zobrist);

            // if the entry depth is greater than current depth, use the stored evaluation as it is more accurate due to deeper search
            if(entry.depth >= depth){
                int entryScore = entry.eval;

                // adjust to current depth if a score is within checkmate threshold and checkmate is found
                if(entryScore > CHECKMATE_THRESHOLD){
                    entryScore += searchPly;
                }
                else if(entryScore < -CHECKMATE_THRESHOLD){
                    entryScore -= searchPly;
                }

                if(entry.entry_TYPE == TranspositionTable.EXACT_TYPE){
                    cutOffCount++;
                    return entry.eval;
                }
                else if(entry.entry_TYPE == TranspositionTable.LOWERBOUND_TYPE){
                    alpha = Math.max(alpha, entryScore);
                }
                else if(entry.entry_TYPE == TranspositionTable.UPPERBOUND_TYPE){
                    beta = Math.min(beta, entryScore);
                }
                if(alpha >= beta){
                    cutOffCount++;
                    return entryScore;
                }
            }
        }

        // every 32767 (in binary: 0b111111111111111) nodes, check for UCI commands
        if((nodeCount & 32767) == 0){
            listen();
        }

        // do not enter quiescence search while in check
        boolean isKingChecked = board.isKingChecked();

        // Check extension
        if(isKingChecked){
            depth++;
        }

        // evaluate the positions after doing a quiescence search to remove horizon effect (search all captures)
        if(depth <= 0){
            ply = searchPly;
            return quiescenceSearch(alpha, beta);
        }

        nodeCount++;

        // static null move pruning
        if(!isKingChecked && !isPV && beta > -CHECKMATE_THRESHOLD){
            int staticEval = EvalUtilities.evaluate(board);
            int scoreMargin = STATIC_NULL_MOVE_PRUNING_MARGIN * depth;
            if((staticEval - scoreMargin) >= beta){
                return beta;
            }
        }

        // null move pruning
        if(depth >= 3 && !isPV && !isDoingNullMove && !isKingChecked && !isEndGame()){
            int reduction;
            if(depth > 6){  // we can afford to reduce more if there is still a lot of depth to search
                reduction = DEEPER_REDUCTION_CONSTANT;
            }
            else{
                reduction = STANDARD_REDUCTION_CONSTANT;
            }
            Move nullMove = new Move(board, (short) 0);
            isDoingNullMove = true;
            nullMove.makeNullMove();
            int score = -negamax(depth - 1 - reduction, searchPly + 1, -beta, -beta + 1);
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

        // extended futility pruning
        boolean enableFutilityPruning = false;
        if(!isPV && depth <= 3 && !isKingChecked && alpha < CHECKMATE_THRESHOLD){
            // do not do futility pruning when in check or if last move was a capture
            if(board.getPreviousMove() != null && !MoveGenerator.isCapture(board.getPreviousMove().getEncodedMove())){
                // static evaluation
                int staticEval = EvalUtilities.evaluate(board);
                // test if static evaluation + a margin is better than alpha, if it is not, prune last ply
                if ((staticEval + futilityMargin[depth]) <= alpha){
                    enableFutilityPruning = true;
                }
            }
        }

        ArrayList<Short> encodedMoves = board.getAllLegalMoves();

        // Game has ended
        if(encodedMoves.size() == 0){
            // checkmate found
            if(isKingChecked){
                return -CHECKMATE_SCORE + searchPly;
            }
            // draw (stalemate)
            return DRAW_SCORE - CONTEMPT_FACTOR;
        }

        // if we are following the previous pv line, enable pv scoring for move ordering
        // to prioritise searching the PV moves
        if(followPVLine){
            followPVLine = false;

            // look through the move list to check if we have a pv move.
            for(Short moves : encodedMoves){
                // if PV move is found, enable pv move scoring for move ordering and continue following PV line
                if (moves == PVMoves[0][searchPly]) {
                    followPVLine = true;
                    pvMoveScoring = true;
                    break;
                }
            }
        }

        short bestMove = 0;
        int bestScore = Integer.MIN_VALUE, moveCount = 0, searchedScore;

        // set to check for fail-low node
        byte moveFlag = TranspositionTable.UPPERBOUND_TYPE;

        for (Short encodedMove : MoveOrdering.orderMoves(encodedMoves, this, searchPly)) {
            moveCount++;
            Move move = new Move(board, encodedMove);
            move.makeMove();

            // if this is a pv move, do a full search
            if (moveCount == 1) {
                searchedScore = -negamax(depth - 1, searchPly + 1, -beta, -alpha);
            } else {
                boolean deliversCheck = board.isKingChecked();
                if (!deliversCheck && enableFutilityPruning
                        && !MoveGenerator.isCapture(encodedMove)
                        && !MoveGenerator.isPromotion(encodedMove)){
                    // prune this move if futility pruning is enabled and if the move is not a capture/promotion and does not deliver check.
                    move.unMake();
                    continue;
                }

                // late move reductions
                if (depth >= REDUCTION_LIMIT && moveCount > 1
                        && !MoveGenerator.isPromotion(encodedMove)
                        && !MoveGenerator.isCapture(encodedMove)
                        && !isKingChecked) {

                    // do reduce search based on reduction factor with a narrowed window
                    int reduction = REDUCTION_TABLE[depth][moveCount];
                    searchedScore = -negamax(depth - 1 - reduction, searchPly + 1, -alpha - 1, -alpha);
                } else {
                    // do a full-depth search
                    searchedScore = alpha + 1; // a trick to ensure full-depth search is continued
                }

                // PVS search
                if (searchedScore > alpha) {
                    searchedScore = -negamax(depth - 1, searchPly + 1, -alpha - 1, -alpha);

                    // re-search the move
                    if (searchedScore > alpha && searchedScore < beta) {
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
                // a better move has been found, switch to storing exact evaluation
                moveFlag = TranspositionTable.EXACT_TYPE;

                // if the move is a quiet move, store as history move
                if(!MoveGenerator.isCapture(encodedMove)){
                    // store history move data
                    historyMoves[MoveGenerator.getStart(encodedMove)][MoveGenerator.getEnd(encodedMove)] += (depth * depth);
                }

                // write PV move
                PVMoves[searchPly][searchPly] = encodedMove;
                // copy move from deeper ply into a current ply's line
                if (PVLength[searchPly + 1] - searchPly + 1 >= 0){
                    System.arraycopy(PVMoves[searchPly + 1], searchPly + 1, PVMoves[searchPly], searchPly + 1, PVLength[searchPly + 1] - searchPly + 1);
                }
                PVLength[searchPly] = PVLength[searchPly + 1];
            }

            // fail-hard beta cut off has occurred
            if(alpha >= beta) {
                // store in transposition table
                TT.recordEntry(board.getZobristHash(), encodedMove, (byte) depth, beta, TranspositionTable.LOWERBOUND_TYPE);

                // if the move that causes a cutoff is a quiet move (not a capture) store move as killer moves
                if(!MoveGenerator.isCapture(encodedMove)){
                    // store killer move
                    killerMoves[1][searchPly] = killerMoves[0][searchPly];
                    killerMoves[0][searchPly] = encodedMove;
                }

                return beta;
            }
        }

        // store the best move at current position
        TT.recordEntry(board.getZobristHash(), bestMove, (byte) depth, bestScore, moveFlag);

        return bestScore;
    }

    /**
     * Evaluates the current position on the board by continuing to search all possible capture lines to reduce horizon effect
     * i.e. Prevents the AI from blundering a piece due to search being cut at a certain depth causing it to not "see" opponent attacks
     */
    private int quiescenceSearch(int alpha, int beta){
        // every 32767 (in binary: 0b111111111111111) nodes, check for UCI commands
        if((nodeCount & 32767) == 0){
            listen();
        }

        if(isDraw(board)){
            return 0;
        }

        int stand_pat = EvalUtilities.evaluate(board);

        // ensure that the ply searched is not greater than max ply due to extensions
        if(ply > MAX_PLY - 1){
            return stand_pat;
        }

        if(stand_pat >= beta){
            return beta; // fail hard
        }

        // Delta pruning
        int BIG_DELTA = Queen.QUEEN_MG_VALUE; // queen value

        if (stand_pat < (alpha - BIG_DELTA)) {
            return alpha;
        }
        nodeCount++;

        if(alpha < stand_pat){
            alpha = stand_pat;
        }

        ArrayList<Short> captureMoves = board.getAllCaptures();

        for (Short encodedMove : MoveOrdering.orderMoves(captureMoves, this, ply)) {

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

            if(searchedScore > alpha){
                alpha = searchedScore;
                // cut-off has occurred
                if(searchedScore >= beta) {
                    return beta;
                }
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
        PVMoves = new short[MAX_PLY][MAX_PLY];
        PVLength = new int[MAX_PLY];
        followPVLine = false;
        pvMoveScoring = false;
        isDoingNullMove = false;
    }

    /**
     * Checks if the game is in the end game phase
     * @return true when there are less than 7 minor and major pieces (excluding x2 kings) on the board
     */
    private boolean isEndGame(){
        return (board.getPieceList(true).getCount() + board.getPieceList(false).getCount()) <= 9;
    }

    private static int calculateReduction(int depth, int moveCount){
        return (int) ((1 / 1.95) * Math.log(depth) * Math.log(moveCount));
    }

    private boolean isDraw(Board board){
        long zobrist = board.getZobristHash();

        // check for fifty move rule (>= 100 half moves)
        if(board.getHalfMoveClock() >= 100){
            return true;
        }

        // check for 3 move repetition
        else if(board.repetitionHistory.containsKey(zobrist) && (board.repetitionHistory.get(zobrist) >= 1)){
            return true;
        }

        // check for insufficient material draw
        int whitePieceCount = board.getWhitePieces().getCount();
        int blackPieceCount = board.getBlackPieces().getCount();

        // King vs King
        if(whitePieceCount == 1 && blackPieceCount == 1){
            return true;
        }

        // King + Bishop/Knight vs King
        else if((whitePieceCount == 1 && blackPieceCount == 2) || (whitePieceCount == 2 && blackPieceCount == 1)){
            PieceList pieceList;
            if(whitePieceCount == 2){
                pieceList = board.getWhitePieces();
            }
            else{
                pieceList = board.getBlackPieces();
            }

            // check if the other piece remaining is a bishop or knight
            for(int i = 0; i < 2; i++){
                Piece piece = board.getTile(pieceList.occupiedTiles[i]).getPiece();
                if(piece.isKnight() || piece.isBishop()){
                    return true;
                }
            }
        }

        // King + Bishop vs King + Bishop (same coloured bishops)
        else if(whitePieceCount == 2 && blackPieceCount == 2){
            PieceList whitePieces = board.getWhitePieces();
            PieceList blackPieces = board.getBlackPieces();

            int whiteBishopPosition = -1, blackBishopPosition = -1;

            // check if the other piece remaining is a bishop or knight
            for(int i = 0; i < 2; i++){
                Piece whitePiece = board.getTile(whitePieces.occupiedTiles[i]).getPiece();
                Piece blackPiece = board.getTile(blackPieces.occupiedTiles[i]).getPiece();

                if(whitePiece.isBishop()){
                    whiteBishopPosition = whitePiece.getPosition();
                }
                if(blackPiece.isBishop()){
                    blackBishopPosition = blackPiece.getPosition();
                }
            }

            // if both remaining pieces are bishops
            if(whiteBishopPosition != -1 && blackBishopPosition != -1){
                // if both bishops sit on same coloured squares, it is a draw
                return isLightSquare(whiteBishopPosition) == isLightSquare(blackBishopPosition);
            }
        }
        return false;
    }

    private boolean isLightSquare(int position){
        int row = (position - (position % 8)) / 8;
        int col = position % 8;
        return (row + col + 1) % 2 != 0;
    }

    /**
     * Unit Testing
     */
    public static void main(String[] args) throws IOException {
        Board board = new Board();
        //board.init("8/8/2p3kp/ppp5/6PK/2P4P/P1P5/8 w - - 0 1");
        // mate in 4
        //board.init("k7/4RP2/n1p2r2/8/p2N4/2P3Pp/1P5P/6K1 w - - 3 46");
        board.init(FENUtilities.trickyFEN);
        //board.init("r1bq1rk1/2p1bppp/p1np1n2/1p2p3/3PP3/1B3N2/PPP2PPP/RNBQR1K1 w - - 0 9");
        //board.init("8/7k/5Q2/8/8/8/8/1K6 b - - 0 1");
        Search search = new Search(board, new TranspositionTable());
        search.depthSearch(11);
        System.exit(0);

        /*
         * TESTING FOR EVALUATION OF INDIVIDUAL MOVES
         */

//        long start = System.currentTimeMillis();
//        for(Short move : board.getAllLegalMoves()){
//            Move m = new Move(board, move);
//            m.makeMove();
//            int score = search.negamax(8, 0, -INFINITY, INFINITY);
//            m.unMake();
//
//            System.out.println(MoveGenerator.toString(move) + " " + score);
//        }
//        long end = System.currentTimeMillis();
//        System.out.println("Time Taken: " + (end - start));
    }
}
