import java.util.ArrayList;

public class Rook extends Piece{

    /*
     * Move Rules:
     * 1) Rook only moves/attacks vertically or horizontally (up, down, left, right)
     * 2) TODO Castling. If rook is moved, whichever side the rook is on loses castling rights
     *
     * Rook moves by sliding. Check if piece is blocked by any piece. If piece is enemy, attack is possible.
     * Similar implementation to a Bishop
     */

    private static final int ROOK_VALUE = 5;
    private static final int[] direction = {-1, 1, -8, 8}; // direction of a rook

    public Rook(boolean isWhite, int position, Board b){
        super(isWhite, position, b);
    }

    @Override
    public ArrayList<Integer> getDefendingSquares(){
        ArrayList<Integer> list = new ArrayList<>();
        int counter, end;
        for(int i = 0; i < 4; i++){
            // go in direction[i]
            end = this.position + direction[i];
            counter = 0;  // max is 7 moves in any direction
            while(isValidMove(this.position, end) && counter < 7){ // while within board range and not blocked
                counter++;
                list.add(end);
                if(super.board.getTile(end).isOccupied()){
                    break;  // if piece is blocked, break from loop
                }
                end += direction[i];    // continue in direction of path
            }
        }
        return list;
    }

    @Override
    public boolean isValidMove(int start, int end) {
        if(start < 0 || start > 63 || end < 0 || end > 63){
            return false;   // out of bound
        }
        // check if points are moving on the same line
        // points on same vertical line have same x coordinates
        // points on same horizontal line have same y coordinates
        return getCol(start) == getCol(end) || getRow(start) == getRow(end);
    }

    @Override
    public int getValue(){  // value of a rook
        return ROOK_VALUE;
    }

    @Override
    public String toString(){
        return "R";
    }
}
