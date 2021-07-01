public class Test {
    private final Board board;

    public Test(Board board){
        this.board = board;
    }

    private int MoveGeneratorTest(int depth){
        if(depth == 0){
            return 1;
        }
        int count = 0;

        for(short encodedMoves : board.getAllLegalMoves()){
            Move move = new Move(board, MoveGenerator.getStart(encodedMoves), MoveGenerator.getEnd(encodedMoves));
            move.makeMove();
            count+= MoveGeneratorTest(depth - 1);
            move.unMake();
        }

        return count;
    }
    public static void main(String[] args) {
        Board board = new Board();
        board.init(FENUtilities.startFEN);
        Test test = new Test(board);
        int depth = 6;

        long start = System.currentTimeMillis();
        int ans = test.MoveGeneratorTest(depth);
        long finish = System.currentTimeMillis();
        long timeElapsed = finish - start;

        float convertTime = (float) timeElapsed / 1000;
        float NPS = (float) ans / convertTime;
        System.out.println("Seach to Depth " + depth + ": " + ans);
        System.out.println("Time Elapsed: " + timeElapsed);
        System.out.println("NPS: " + NPS);
    }
}
