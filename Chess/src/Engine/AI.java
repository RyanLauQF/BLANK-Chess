import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

public class AI {
    // opening book used by AI
    protected final boolean isWhite;
    protected final Board board;
    private final OpeningTrie openingBook;
    private boolean isUsingOpeningBook;
    private int moveNum;

    private int depthCount;
    private int search;
    private int maxDepth;
    private int nodeCount;


    private static final int REDUCTION_CONSTANT = 2;

    public AI(boolean isWhite, Board board) throws IOException {
        this.isWhite = isWhite;
        this.board = board;
        this.openingBook = new OpeningTrie(isWhite);    // builds the opening book for the AI
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
            searchDepth += 3;
        }
        search = searchDepth;
        maxDepth = 0;
        nodeCount = 0;
        long start = System.currentTimeMillis();
        ArrayList<Short> moves = board.getAllLegalMoves();
        short bestMove = 0;
        int bestMoveScore = Integer.MIN_VALUE;
        for(Short move : MoveOrdering.orderMoves(moves, board)){
            Move movement = new Move(board, move);
            movement.makeMove();
            int score = -searchBestMove(searchDepth - 1, Integer.MIN_VALUE + 1, Integer.MAX_VALUE);
            //System.out.println(score + " " + MoveGenerator.getStart(move) + " " + MoveGenerator.getEnd(move));
            if(score > bestMoveScore){
                bestMoveScore = score;
                bestMove = move;
            }
            movement.unMake();
        }
        long finish = System.currentTimeMillis();
        long timeElapsed = finish - start;
        float convertTime = (float) timeElapsed / 1000;
        System.out.println("---------------------------------");
        System.out.println("Depth: " + searchDepth);
        System.out.println("Max Depth Searched: " + maxDepth);
        System.out.println("Nodes Searched: " + nodeCount);
        System.out.println("Best Move: " + FENUtilities.convertIndexToRankAndFile(MoveGenerator.getStart(bestMove)) + "-" + FENUtilities.convertIndexToRankAndFile(MoveGenerator.getEnd(bestMove)));
        System.out.println("Time Elapsed: " + convertTime + " seconds");
        System.out.println("---------------------------------");
        return bestMove;
    }

    public int searchBestMove(int depth, int alpha, int beta){
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

        ArrayList<Short> encodedMoves = board.getAllLegalMoves();
        if(encodedMoves.size() == 0){
            nodeCount++;
            if(board.isKingChecked()){
                return -Integer.MAX_VALUE;  // checkmate found
            }
            return 0;
        }
        int bestScore = Integer.MIN_VALUE + 1;
        for (Short encodedMove : MoveOrdering.orderMoves(encodedMoves, board)) {
            Move move = new Move(board, encodedMove);
            move.makeMove();
            int searchedScore = -searchBestMove(depth - 1, -beta, -alpha);
            move.unMake();
            if(searchedScore >= bestScore) bestScore = searchedScore;
            if(bestScore > alpha) alpha = bestScore;
            if(alpha >= beta) return alpha;
        }
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
        int BIG_DELTA = 900; // queen value
        if (stand_pat < (alpha - BIG_DELTA)) {
            return stand_pat;
        }

        if(alpha < stand_pat){
            alpha = stand_pat;
        }

        depthCount++;
        ArrayList<Short> captureMoves = board.getAllCaptures();
        for (Short encodedMove : MoveOrdering.orderMoves(captureMoves, board)) {
            Move move = new Move(board, encodedMove);
            move.makeMove();
            int searchedScore = -quiescenceSearch(-beta, -alpha, depth - 1);
            move.unMake();
            if(searchedScore >= beta) return beta;
            if(searchedScore > alpha) alpha = searchedScore;
        }
        return alpha;
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
        board.init(FEN);
        AI testAI = new AI(true, board);
//        Move movement = new Move(board, MoveGenerator.generateMove(51, 35, 1));
//        movement.makeMove();
//        short move = testAI.getMove();
//        System.out.println(MoveGenerator.getStart(move) + " " + MoveGenerator.getEnd(move));
//        System.out.println("Book size: " + testAI.openingBook.size());

        int depth = 5;  // 16469630 54.663

        long start = System.currentTimeMillis();
        short move = testAI.getBestMove(depth, false);
        long finish = System.currentTimeMillis();
        long timeElapsed = finish - start;
        float convertTime = (float) timeElapsed / 1000;

        System.out.println("Seach to Depth " + depth + ": " + MoveGenerator.getStart(move) + " " + MoveGenerator.getEnd(move));
        System.out.println("Time Elapsed: " + convertTime + " seconds");
    }
}
