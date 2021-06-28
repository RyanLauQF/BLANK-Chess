public class Test {
    private Board board;

    public Test(Board board){
        this.board = board;
    }

    private int MoveGeneratorTest(int depth){
        if(depth == 0){
            return 1;
        }
        Board boardCopy;
        int count = 0;
        if(board.isWhiteTurn()){
            for(int pieceLocation : board.getWhitePieces()){
                for(int moves : board.getTile(pieceLocation).getPiece().getLegalMoves()){
                    boardCopy = board;
                    board.move(pieceLocation, moves);
                    count += MoveGeneratorTest(depth - 1);
                    board = boardCopy;
                }
            }
        }
        else{
            for(int pieceLocation : board.getBlackPieces()){
                for(int moves : board.getTile(pieceLocation).getPiece().getLegalMoves()){
                    boardCopy = board;
                    board.move(pieceLocation, moves);
                    count += MoveGeneratorTest(depth - 1);
                    board = boardCopy;
                }
            }
        }

        return count;
    }
    public static void main(String[] args) {
        Board board = new Board();
        board.init(FENUtilities.startFEN);
        Test test = new Test(board);

        test.MoveGeneratorTest(1);
    }
}
