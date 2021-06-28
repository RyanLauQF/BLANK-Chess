import java.util.ArrayList;

public class Bishop extends Piece{

    /*
     * Move Rules:
     * 1) Bishop only moves/attacks diagonally (top right, bot right, top left, bot left)
     *
     * Bishop moves by sliding. Check if piece is blocked by any piece. If piece is enemy, attack is possible.
     * Similar implementation to a Rook
     */

    private static final int BISHOP_VALUE = 3;
    private static final int[] direction = {-7, 7, -9, 9};  // diagonal paths of a bishop

    public Bishop(boolean isWhite, int position, Board b){
        super(isWhite, position, b);
    }

    @Override
    public ArrayList<Integer> getDefendingSquares(){
        ArrayList<Integer> list = new ArrayList<>();
        // check each diagonal path until it reaches edge of board or get blocked by another piece
        int end;
        for(int i = 0; i < 4; i++){
            // go in diagonal along direction[i]
            end = this.position + direction[i];
            while(isValidMove(this.position, end)){ // while within board range and not blocked
                list.add(end);
                if(super.board.getTile(end).isOccupied()){  // break travel along direction if piece is blocked
                    break;
                }
                end += direction[i];    // continue in direction of diagonal
            }
        }
        return list;
    }

    @Override
    public boolean isValidMove(int start, int end) {
        if(start < 0 || start > 63 || end < 0 || end > 63){
            return false;   // out of bound
        }
        // check if start/end is on the same diagonal (points on same diagonal will have same x and y offsets)
        return Math.abs(getRow(start) - getRow(end)) == Math.abs(getCol(start) - getCol(end));
    }

    @Override
    public int getValue(){  // value of a bishop
        return BISHOP_VALUE;
    }

    @Override
    public String toString(){
        return "B";
    }
}
