import java.util.ArrayList;

public class Player {
    private final boolean isWhite;
    private ArrayList<Integer> piecesLeft;

    private Board board;
    private int score;

    Player(boolean isWhite, Board board){
        this.isWhite = isWhite;
        this.board = board;
        this.score = 0;
        if(isWhite){
            this.piecesLeft = board.getWhitePieces();
        }
        else{
            this.piecesLeft = board.getBlackPieces();
        }
        // Set the initial score of the player
        for(int pieces : getPiecesLeft()){
            this.score += getBoard().getTile(pieces).getPiece().getValue();
        }
    }

    public int getScore(){
        return score;
    }

    private Board getBoard(){
        return board;
    }

    private ArrayList<Integer> getPiecesLeft(){
        return piecesLeft;
    }

    /**
     * unit testing
     */
    public static void main(String[] args) {

    }
}
