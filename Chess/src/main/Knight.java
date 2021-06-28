import java.util.ArrayList;

public class Knight extends Piece{

    /*
     * Move Rules:
     * 1) Knight moves/attacks in an L shape
     * 2) Max number of moves is 8.
     *
     * Knight moves by jumping over other pieces. Check if piece landed on is an allied piece.
     */

    private static final int KNIGHT_VALUE = 3;
    // all possible moves of a knight based on a 1D array board where index 0 is at the top left.
    private static final int[] direction = {-6, 6, -10, 10, -15, 15, -17, 17};

    public Knight(boolean isWhite, int position, Board b){
        super(isWhite, position, b);
    }

    @Override
    public ArrayList<Integer> getDefendingSquares(){
        ArrayList<Integer> list = new ArrayList<>();
        for(int i = 0; i < 8; i++){ // check all direction
            if(isValidMove(this.getPosition(), this.getPosition() + direction[i])){
                list.add(this.getPosition() + direction[i]);    // add to list of controlled squares
            }
        }
        return list;
    }

    @Override
    public boolean isValidMove(int start, int end) {
        if(start < 0 || start > 63 || end < 0 || end > 63) {
            return false;   // out of bounds
        }
        // check if direction of movement is possible based on knights position
        // get the sum of differences between end position and start position to check L-shape movement
        // for an L-shape movement, the sum must be equal to 3.
        // return true if sum == 3
        return Math.abs(getRow(start) - getRow(end)) + Math.abs(getCol(start) - getCol(end)) == 3;
    }

    @Override
    public int getValue(){  // value of a knight
        return KNIGHT_VALUE;
    }

    @Override
    public String toString(){
        return "N";
    }
}
