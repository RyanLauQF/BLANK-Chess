import java.util.ArrayList;

public class Queen extends Piece{

    /*
     * Move Rules:
     * 1) Queen can move/attack in all directions (diagonally, horizontally and vertically)
     *
     * Queen moves by sliding. Check if piece is blocked by any piece. If piece is enemy, attack is possible.
     * Similar implementation to a Bishop + Rook movement
     */

    private static final int QUEEN_VALUE = 9;
    private static final int[] direction = {-7, 7, -9, 9, -1, 1, -8, 8};    // index 0 - 3 are diagonal moves, 4 - 7 are straight moves
    private boolean isDiagonal; // change move checking mode between diagonal and straight moves

    public Queen(boolean isWhite, int position, Board b){
        super(isWhite, position, b);
    }

    @Override
    public ArrayList<Integer> getDefendingSquares(){
        ArrayList<Integer> list = new ArrayList<>();
        int counter, end;
        isDiagonal = true;  // check for diagonal moves from index 0 to 3 in direction array
        for(int i = 0; i < 8; i++){ // number of possible directions queen can move
            // go in direction[i]
            end = this.position + direction[i];
            if(i > 3) isDiagonal = false;   // check for straight moves from index 4 to 7 in direction array
            counter = 0;
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
        // check if movement is diagonal or in a straight line
        if(isDiagonal){ // diagonal moves checking
            if (Math.abs(getRow(start) - getRow(end)) != Math.abs(getCol(start) - getCol(end))) return false;

        }
        else{   // straight move checking
            if(getCol(start) != getCol(end) && getRow(start) != getRow(end)) return false;
        }
        return true;    // if tile is not occupied
    }

    @Override
    public int getValue(){
        return QUEEN_VALUE;
    }

    @Override
    public String toString(){
        return "Q";
    }
}
