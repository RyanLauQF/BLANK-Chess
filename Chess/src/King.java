import java.util.ArrayList;

public class King extends Piece {

    /*
     * Move Rules:
     * 1) King can move/attack in all directions (diagonally, horizontally and vertically) by only 1 tile
     * 2) TODO Castling. Only available if king and rook on either King or Queen side has not moved
     *     and tiles between the 2 pieces are empty. (Check if castling results in a checked position)
     *
     * King moves by jumping. Check if piece is blocked by any piece. If piece is enemy, attack is possible.
     * Similar implementation to a knight
     */

    private static final int KING_VALUE = 100;
    private static final int[] direction = {-9, -8, -7, -1, 1, 7, 8, 9};    // can move 1 tile in any direction

    public King(boolean isWhite, int position, Board b){
        super(isWhite, position, b);
        b.setKingPosition(position);
    }

    @Override
    // get all squares which piece is defending
    public ArrayList<Integer> getDefendingSquares(){
        ArrayList<Integer> list = new ArrayList<>();
        for(int i = 0; i < 8; i++){ // check all direction
            if(isValidMove(this.getPosition(), this.getPosition() + direction[i])){
                list.add(this.getPosition() + direction[i]);
            }
        }
        return list;
    }

    @Override
    public boolean isValidMove(int start, int end) {
        if(start < 0 || start > 63 || end < 0 || end > 63){
            return false;   // out of bound
        }
        int checkValidMove = Math.abs(getRow(start) - getRow(end)) + Math.abs(getCol(start) - getCol(end));
        // if end position is valid, move is legal
        return checkValidMove == 1 || checkValidMove == 2;
    }

    @Override
    public int getValue(){  // value of a king
        return KING_VALUE;
    }

    @Override
    public String toString(){
        return "K";
    }
}
