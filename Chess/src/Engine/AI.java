import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

public class AI {
    // opening book used by AI
    protected final boolean isWhite;
    protected final Board board;
    private final OpeningTrie openingBook;
    private final TranspositionTable TT;
    private boolean isUsingOpeningBook;
    private int moveNum;

    private int depthCount;
    private int search;
    private int maxDepth;
    private int nodeCount;

    public int counter;

    //ArrayList<Short> PVLine;

    private static final int REDUCTION_CONSTANT = 2;

    public AI(boolean isWhite, Board board) throws IOException {
        this.isWhite = isWhite;
        this.board = board;
        this.openingBook = new OpeningTrie(isWhite);    // builds the opening book for the AI
        this.TT = new TranspositionTable();
        this.moveNum = 0;  // number of moves made by this AI
        this.isUsingOpeningBook = true;
    }

    // selects a move when it is the AI turn
    public short getMove(){
        ArrayList<Short> moves = board.getAllLegalMoves();
        Random rand = new Random();
        int randomMove = rand.nextInt(moves.size());
        System.out.println("Making Random Move!");
        return moves.get(randomMove);
    }

    public short getOpeningMove(){
        // convert the move from algebraic notation to an encoded move.
        String PGN_notation = openingBook.getNextMove();
        openingBook.makeMove(PGN_notation); // make the AI's move in opening book
        return PGNExtract.convertNotationToMove(board, isWhite(), PGN_notation);
    }

    public boolean isWhite(){
        return isWhite;
    }

    public short getBestMove(int searchDepth, boolean enableOpeningBook){
        moveNum++;
        // gets opening moves from opening book for the first 8 moves
        if(this.moveNum <= 8 && isUsingOpeningBook && enableOpeningBook){
            Move previousMove = board.getPreviousMove();
            if(previousMove == null){   // first move being made. AI opening book does not need to record opponent move
                System.out.println("Using Opening Book!");
                return getOpeningMove();
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
                    System.out.println("Using Opening Book!");
                    return getOpeningMove();
                }
                else{
                    isUsingOpeningBook = false;
                }
            }
        }
        if(isEndGame()){
            searchDepth += 1;
        }

        search = searchDepth;
        maxDepth = 0;
        nodeCount = 0;
        counter = 0;

        long start = System.currentTimeMillis();
        ArrayList<Short> moves = board.getAllLegalMoves();
        short bestMove = 0;
        int bestMoveScore = Integer.MIN_VALUE;
        for(Short move : MoveOrdering.orderMoves(moves, board, TT)){
            Move movement = new Move(board, move);
            movement.makeMove();
            int score = -searchBestMove(searchDepth - 1, Integer.MIN_VALUE + 1, Integer.MAX_VALUE);
            //System.out.println(FENUtilities.convertIndexToRankAndFile(MoveGenerator.getStart(move)) + "-" + FENUtilities.convertIndexToRankAndFile(MoveGenerator.getEnd(move)) + " " + score);
            if(score > bestMoveScore){
                bestMoveScore = score;
                bestMove = move;
            }
            movement.unMake();
        }
        // record into Transposition table
        TT.store(board.getZobristHash(), bestMove, (byte) searchDepth, bestMoveScore, TranspositionTable.EXACT_TYPE);

        long finish = System.currentTimeMillis();
        long timeElapsed = finish - start;
        float convertTime = (float) timeElapsed / 1000;
        System.out.println("---------------------------------");
        System.out.println("Best Move Score: " + bestMoveScore);
        System.out.println("Depth: " + searchDepth);
        System.out.println("Max Depth Searched: " + maxDepth);
        System.out.println("Nodes Searched: " + nodeCount);
        System.out.println("Best Move: " + FENUtilities.convertIndexToRankAndFile(MoveGenerator.getStart(bestMove)) + "-" + FENUtilities.convertIndexToRankAndFile(MoveGenerator.getEnd(bestMove)));
        System.out.println("Time Elapsed: " + convertTime + " seconds");
        System.out.println("Transposition Table size: " + TT.table.size());
        System.out.println("Transposition Cutoff: " + counter);
        System.out.println("---------------------------------");
        return bestMove;
    }

    public int searchBestMove(int depth, int alpha, int beta){
        long zobrist = board.getZobristHash();
        // obtain transposition table data if current position has already been evaluated before
        if(TT.containsKey(zobrist)){
            TranspositionTable.TTEntry entry = TT.getEntry(zobrist);
            if(entry.depth >= depth){
                if(entry.entry_TYPE == TranspositionTable.EXACT_TYPE){
                    counter++;
                    return entry.eval;
                }
                else if(entry.entry_TYPE == TranspositionTable.LOWERBOUND_TYPE){
                    alpha = Math.max(alpha, entry.eval);
                }
                else if(entry.entry_TYPE == TranspositionTable.UPPERBOUND_TYPE){
                    beta = Math.min(beta, entry.eval);
                }
                if(alpha >= beta){
                    counter++;
                    return entry.eval;
                }
            }
        }

        // check for move repetition
        if(board.repetitionHistory.containsKey(zobrist) && (board.repetitionHistory.get(zobrist) >= 1)){
            // do not play any same position more than once
            return 0;
        }

        if(depth == 0){
            //count++;
            depthCount = search;
            return quiescenceSearch(alpha, beta, 2);
        }

        // null move pruning
        boolean isKingChecked = board.isKingChecked();
        if(depth >= 3 && !isKingChecked && !isEndGame()){
            Move nullMove = new Move(board, (short) 0);
            nullMove.makeNullMove();
            int score = -searchBestMove(depth - 1 - REDUCTION_CONSTANT, -beta, -beta + 1);   // set R to 2
            nullMove.unmakeNullMove();
            if (score >= beta){
                return beta;
            }
        }

        // Check extension
        if(isKingChecked){
            depth++;
        }

        ArrayList<Short> encodedMoves = board.getAllLegalMoves();
        if(encodedMoves.size() == 0){
            nodeCount++;
            if(board.isKingChecked()){
                int curr_ply = search - depth;
                return -EvalUtilities.CHECKMATE_SCORE + curr_ply;  // checkmate found
            }
            return 0;
        }

        short currentBestMove = 0;
        int bestScore = Integer.MIN_VALUE + 1;

        for (Short encodedMove : MoveOrdering.orderMoves(encodedMoves, board, TT)) {
            Move move = new Move(board, encodedMove);
            move.makeMove();
            int searchedScore = -searchBestMove(depth - 1, -beta, -alpha);
            move.unMake();

            if(searchedScore >= bestScore){
                bestScore = searchedScore;
                currentBestMove = encodedMove;
            }
            if(bestScore > alpha){
                alpha = bestScore;
            }
            if(alpha >= beta) { // cut off has occurred
                TT.store(board.getZobristHash(), currentBestMove, (byte) depth, alpha, TranspositionTable.LOWERBOUND_TYPE);
                return alpha;
            }
        }

        // store the best move at current position
        TT.store(board.getZobristHash(), currentBestMove, (byte) depth, bestScore, TranspositionTable.UPPERBOUND_TYPE);
        return bestScore;
    }

    private int quiescenceSearch(int alpha, int beta, int depth) {
        int stand_pat = EvalUtilities.evaluate(board);
        if(stand_pat >= beta || depth == 0){
            if(depthCount > maxDepth){
                maxDepth = depthCount;
            }
            depthCount = 0;
            nodeCount++;
            return stand_pat;
        }

        // Delta pruning
        int BIG_DELTA = Queen.QUEEN_VALUE; // queen value
        if (stand_pat < (alpha - BIG_DELTA)) {
            return stand_pat;
        }

        if(alpha < stand_pat){
            alpha = stand_pat;
        }

        depthCount++;
        ArrayList<Short> captureMoves = board.getAllCaptures();
        for (Short encodedMove : MoveOrdering.orderMoves(captureMoves, board, TT)) {
            Move move = new Move(board, encodedMove);
            move.makeMove();
            int searchedScore = -quiescenceSearch(-beta, -alpha, depth - 1);
            move.unMake();
            if(searchedScore >= beta) {
                return beta;
            }
            if(searchedScore > alpha) {
                alpha = searchedScore;
            }
        }
        return alpha;
    }

    public void iterativeDS(int depth){
        for(int curr_depth = 1; curr_depth <= depth; curr_depth++){
            // int score = searchBestMove(curr_depth, Integer.MIN_VALUE + 1, Integer.MAX_VALUE);
            int score = getBestMove(curr_depth, false);
            System.out.println(score);
        }
    }

    /**
     * If there are less than or equal to 7 pieces left, end game is reached
     * @return true if the AI side is now in end game phase
     */
    public boolean isEndGame(){
        return board.getPieceList(isWhite()).getCount() <= 7 && board.getPieceList(!isWhite()).getCount() <= 7;
    }

    /**
     * Unit Testing
     */
    public static void main(String[] args) throws IOException {
        Board board = new Board();
        String FEN = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1";
        board.init("8/2N5/p4p2/R3pkp1/8/Pr3P1P/2K3P1/8 b - - 0 1");
        AI testAI = new AI(false, board);
//        Move movement = new Move(board, MoveGenerator.generateMove(51, 35, 1));
//        movement.makeMove();
//        short move = testAI.getMove();
//        System.out.println(MoveGenerator.getStart(move) + " " + MoveGenerator.getEnd(move));
//        System.out.println("Book size: " + testAI.openingBook.size());

        int depth = 5;

        long start = System.currentTimeMillis();
        short move = testAI.getBestMove(4, false);
        //testAI.iterativeDS(depth);
        long finish = System.currentTimeMillis();
        long timeElapsed = finish - start;
        float convertTime = (float) timeElapsed / 1000;

        System.out.println("Seach to Depth " + depth + ": " + MoveGenerator.getStart(move) + " " + MoveGenerator.getEnd(move));
        System.out.println("Time Elapsed: " + convertTime + " seconds");
    }
}
