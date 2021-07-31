import java.util.ArrayList;

public class Test {
    private final Board board;
    private TranspositionTable TT;
    private long count;
    private int score;

    public Test(Board board){
        this.board = board;
        this.count = 0;
        this.TT = new TranspositionTable();
    }

    private long MoveGeneratorTest(int depth) {
        if(depth == 0){
            return 1;
        }

        count = 0;
        ArrayList<Short> encodedMoves = board.getAllLegalMoves();

        if(depth == 1){
            return encodedMoves.size();
        }

        for (Short encodedMove : encodedMoves) {
            Move move = new Move(board, encodedMove);
            move.makeMove();
            count += MoveGeneratorTest(depth - 1);
            move.unMake();
        }

        return count;
    }

    private long divide(int depth) {
        ArrayList<Short> encodedMoves = board.getAllLegalMoves();
        long total = 0;
        long currCount;
        int start, end;
        for (Short encodedMove : encodedMoves) {
            start = MoveGenerator.getStart(encodedMove);
            end = MoveGenerator.getEnd(encodedMove);
            System.out.print(FENUtilities.convertIndexToRankAndFile(start) + FENUtilities.convertIndexToRankAndFile(end) + " ");
            Move move = new Move(board, encodedMove);
            move.makeMove();
            currCount = MoveGeneratorTest(depth - 1);
            System.out.print(currCount + " \n");
            total += currCount;
            move.unMake();
        }
        return total;
    }

    public int negaMax(int depth){
        if(depth == 0){
            count++;
            return EvalUtilities.evaluate(board);
        }
        int max = Integer.MIN_VALUE;
        ArrayList<Short> encodedMoves = board.getAllLegalMoves();
        for (Short encodedMove : encodedMoves) {
            Move move = new Move(board, encodedMove);
            move.makeMove();
            score = -negaMax(depth - 1);
            move.unMake();
            if(score > max) {
                max = score;
            }
        }
        return max;
    }

    public int alphaBeta(int depth, int alpha, int beta){
        int bestScore = Integer.MIN_VALUE;
        if(depth == 0){
            count++;
            return EvalUtilities.evaluate(board);
        }
        ArrayList<Short> encodedMoves = board.getAllLegalMoves();
        for (Short encodedMove : encodedMoves) {
            Move move = new Move(board, encodedMove);
            move.makeMove();
            score = -alphaBeta(depth - 1, -beta, -alpha);
            move.unMake();
            if(score >= beta){
                return score;
            }
            if(score > bestScore){
                bestScore = score;
                if(score > alpha){
                    alpha = score;
                }
            }
        }
        return bestScore;
    }

    public int searchBestMove(int depth, int alpha, int beta){
        if(depth == 0){
            return quiescenceSearch(alpha, beta, 2);
            //count++;
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
        for (Short encodedMove : MoveOrdering.orderMoves(encodedMoves, board, TT)) {
            Move move = new Move(board, encodedMove);
            move.makeMove();
            int searchedScore = -searchBestMove(depth - 1, -beta, -alpha);
            move.unMake();
            if(searchedScore >= bestScore) bestScore = searchedScore;
            if(bestScore > alpha) alpha = bestScore;
            if(bestScore >= beta) break;
        }
        return alpha;
    }

    private int quiescenceSearch(int alpha, int beta, int depth) {
//        count++;
        int stand_pat = EvalUtilities.evaluate(board);
        if(stand_pat >= beta || depth == 0){
//            System.out.println(count);
//            count = 0;
            count++;
            return stand_pat;
        }
        if(alpha < stand_pat){
            alpha = stand_pat;
        }

        ArrayList<Short> encodedMoves = board.getAllCaptures();
        for (Short encodedMove : MoveOrdering.orderMoves(encodedMoves, board, TT)) {
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

    /*
        UNIT TESTING FOR MOVE GENERATION
     */
    public static void main(String[] args) throws InterruptedException {
//***** STANDARD DEBUGGING WITH PERFT SPEED *****//
//-------------------------------------------------
        Board board = new Board();
        String FEN = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w - - 0 1";
        board.init(FEN);
        Test test = new Test(board);
        int depth = 5;

        long start = System.currentTimeMillis();
        long ans = test.divide(depth);
        //long ans = test.MoveGeneratorTest(depth);

        //int ans = test.searchBestMove(depth, Integer.MIN_VALUE, Integer.MAX_VALUE);
        //int ans = test.alphaBeta(depth, Integer.MIN_VALUE, Integer.MAX_VALUE);
        //int ans = test.negaMax(depth);

        long finish = System.currentTimeMillis();
        long timeElapsed = finish - start;

        float convertTime = (float) timeElapsed / 1000;
        double NPS = (double) ans / convertTime;
        System.out.println("Seach to Depth " + depth + ": " + ans);
        System.out.println("Time Elapsed: " + convertTime + " seconds");
        //System.out.println(test.count);
        System.out.println("NPS: " + NPS);
        // System.out.println(test.counter);


//***** USED FOR PERFT TREE DEBUGGING WITH STOCKFISH *****//
//----------------------------------------------------------
//        Board board = new Board();
//        if(args.length < 2){
//            throw new IllegalArgumentException();
//        }
//        String d = args[1];
//        int depth = Integer.parseInt(d);
//        String FEN = args[2];
//        board.init(FEN);
//        Test test = new Test(board);
//        long ans = test.divide(depth);
//        System.out.println();
//        System.out.print(ans);
    }
}
