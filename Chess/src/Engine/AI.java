import java.util.ArrayList;
import java.util.Random;

public class AI {
    private final boolean isWhite;
    private final Board board;

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

    public static void main(String[] args) {

    }
}
