public class Chess {
    private final Board board;

    public Chess(){
        // initiate board
        board = new Board();
        board.init(FENUtilities.startFEN);  // default chess starting FEN notation
    }

    public Chess(String FEN){
        // initiate board
        board = new Board();
        board.init(FEN);
    }

    public static void main(String[] args) {

    }
}


