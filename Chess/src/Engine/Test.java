import java.awt.datatransfer.SystemFlavorMap;

public class Test {
    private Board board;

    public Test(Board board){
        this.board = board;
    }

    private int MoveGeneratorTest(int depth){
        if(depth == 0){
            return 1;
        }
        int count = 0;
        if(board.isWhiteTurn()){
            Integer[] whitePieces = new Integer[board.getWhitePieces().size()];
            board.getWhitePieces().toArray(whitePieces);
            for(int pieceLocation : whitePieces){
                for(int moves : board.getTile(pieceLocation).getPiece().getLegalMoves()){
                    Move move = new Move(board, pieceLocation, moves);
                    move.makeMove();
                    count += MoveGeneratorTest(depth - 1);
                    move.unMake();
                }
            }
        }
        else{
            Integer[] blackPieces = new Integer[board.getBlackPieces().size()];
            board.getBlackPieces().toArray(blackPieces);
            for(int pieceLocation : blackPieces){
                for(int moves : board.getTile(pieceLocation).getPiece().getLegalMoves()){
                    Move move = new Move(board, pieceLocation, moves);
                    move.makeMove();
                    count += MoveGeneratorTest(depth - 1);
                    move.unMake();
                }
            }
        }

        return count;
    }
    public static void main(String[] args) {
        Board board = new Board();
        board.init(FENUtilities.startFEN);
        Test test = new Test(board);
        int depth = 4;

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
