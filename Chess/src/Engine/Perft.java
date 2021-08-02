import java.util.ArrayList;

public class Perft {
    private final Board board;
    private long count;

    public Perft(Board board) {
        this.board = board;
        this.count = 0;
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

    /*
     * UNIT TESTING FOR MOVE GENERATION USING PERFT RESULTS
     */
    public static void main(String[] args) {
//***** STANDARD DEBUGGING WITH PERFT *****//
//-------------------------------------------------
        Board board = new Board();
        String FEN = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w - - 0 1";
        board.init(FEN);
        Perft test = new Perft(board);
        int depth = 5;

        long start = System.currentTimeMillis();
        long ans = test.divide(depth);
        long finish = System.currentTimeMillis();
        long timeElapsed = finish - start;

        float convertTime = (float) timeElapsed / 1000;
        double NPS = (double) ans / convertTime;
        System.out.println("Seach to Depth " + depth + ": " + ans);
        System.out.println("Time Elapsed: " + convertTime + " seconds");
        System.out.println("NPS: " + NPS);


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
//        Perft test = new Perft(board);
//        long ans = test.divide(depth);
//        System.out.println();
//        System.out.print(ans);
    }
}
