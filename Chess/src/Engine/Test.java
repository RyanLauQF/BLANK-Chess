import java.util.ArrayList;

public class Test {
    private final Board board;

    public Test(Board board){
        this.board = board;
    }

    private long MoveGeneratorTest(int depth){
        long count = 0;
        ArrayList<Short> encodedMoves = board.getAllLegalMoves();

        if(depth == 1){
            return encodedMoves.size();
        }

        for (Short encodedMove : encodedMoves) {
            Move move = new Move(board, MoveGenerator.getStart(encodedMove), MoveGenerator.getEnd(encodedMove));
            move.makeMove();
            count += MoveGeneratorTest(depth - 1);
            move.unMake();
        }
        return count;
    }
    public static void main(String[] args) {
        Board board = new Board();
        // String FEN = "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1";
        board.init(FENUtilities.startFEN);
        Test test = new Test(board);
        int depth = 7;

        long start = System.currentTimeMillis();
        long ans = test.MoveGeneratorTest(depth);
        long finish = System.currentTimeMillis();
        long timeElapsed = finish - start;

        float convertTime = (float) timeElapsed / 1000;
        double NPS = (double) ans / convertTime;
        System.out.println("Seach to Depth " + depth + ": " + ans);
        System.out.println("Time Elapsed: " + convertTime + " seconds");
        System.out.println("NPS: " + NPS);
    }
}
