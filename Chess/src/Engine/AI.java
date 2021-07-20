import java.util.ArrayList;
import java.util.Random;

public class AI {
    private final boolean isWhite;
    private final Board board;
    private int count;

    public AI(boolean isWhite, Board board){
        this.isWhite = isWhite;
        this.board = board;
    }

    // selects a move when it is the AI turn
    public short getMove(){
        ArrayList<Short> moves = board.getAllLegalMoves();
        Random rand = new Random();
        int randomMove = rand.nextInt(moves.size());
        return moves.get(randomMove);
    }

    public boolean getTurn(){
        return isWhite;
    }

    public short getBestMove(int searchDepth){
        count = 0;
        long start = System.currentTimeMillis();
        ArrayList<Short> moves = board.getAllLegalMoves();
        short bestMove = 0;
        int bestMoveScore = Integer.MIN_VALUE;
        for(Short move : MoveOrdering.orderMoves(moves, board)){
            Move movement = new Move(board, move);
            movement.makeMove();
            int score = -searchBestMove(searchDepth - 1, Integer.MIN_VALUE, Integer.MAX_VALUE);
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
        System.out.println("Nodes Searched: " + count);
        System.out.println("Best Move: " + FENUtilities.convertIndexToRankAndFile(MoveGenerator.getStart(bestMove)) + "-" + FENUtilities.convertIndexToRankAndFile(MoveGenerator.getEnd(bestMove)));
        System.out.println("Time Elapsed: " + convertTime + " seconds");
        System.out.println("---------------------------------");
        return bestMove;
    }

    public int searchBestMove(int depth, int alpha, int beta){
        if(depth == 0){
            //count++;
            return quiescenceSearch(alpha, beta, 2);
            //return EvalUtilities.evaluate(board);
        }
        ArrayList<Short> encodedMoves = board.getAllLegalMoves();
        if(encodedMoves.size() == 0){
            //count++;
            if(board.isKingChecked()){
                return -Integer.MAX_VALUE;  // checkmate found
            }
            return 0;
        }
        int bestScore = Integer.MIN_VALUE;
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
            count++;
            return stand_pat;
        }
        if(alpha < stand_pat){
            alpha = stand_pat;
        }

        ArrayList<Short> encodedMoves = board.getAllLegalMoves();
        for (Short encodedMove : MoveOrdering.quiescenceOrdering(encodedMoves, board)) {
            if(MoveGenerator.isCapture(encodedMove)){
                Move move = new Move(board, encodedMove);
                move.makeMove();
                int searchedScore = -quiescenceSearch(-beta, -alpha, depth - 1);
                move.unMake();
                if(searchedScore >= beta) return beta;
                if(searchedScore > alpha) alpha = searchedScore;
            }
        }
        return alpha;
    }

    /**
     * Unit Testing
     */
    public static void main(String[] args) {
        Board board = new Board();
        String FEN = "r3k2r/p1ppqpb1/Bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPB1PPP/R3K2R b - - 0 1";
        board.init(FEN);
        AI testAI = new AI(false, board);
        int depth = 5;

        long start = System.currentTimeMillis();
        short move = testAI.getBestMove(depth);
        long finish = System.currentTimeMillis();
        long timeElapsed = finish - start;
        float convertTime = (float) timeElapsed / 1000;

        System.out.println("Seach to Depth " + depth + ": " + MoveGenerator.getStart(move) + " " + MoveGenerator.getEnd(move));
        System.out.println("Time Elapsed: " + convertTime + " seconds");
    }
}
