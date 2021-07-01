import java.util.ArrayList;

public class Player {
    private final boolean isWhite;
    private PieceList piecesLeft;

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
        for(int i = 0; i  < getPiecesLeft().getCount(); i++){
            this.score += getBoard().getTile(getPiecesLeft().occupiedTiles[i]).getPiece().getValue();
        }
    }

    public int getScore(){
        return score;
    }

    private Board getBoard(){
        return board;
    }

    private PieceList getPiecesLeft(){
        return piecesLeft;
    }

    /**
     * unit testing
     */
    public static void main(String[] args) {

    }
}
