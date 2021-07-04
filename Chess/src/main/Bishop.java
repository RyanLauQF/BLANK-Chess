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

    public Bishop(boolean isWhite, int position, Board b){
        super(isWhite, position, b);
        this.type = PieceType.BISHOP;
    }

    @Override
    public ArrayList<Short> getDefendingSquares(){
        ArrayList<Short> list = new ArrayList<>();
        // check each diagonal path until it reaches edge of board or get blocked by another piece
        int end, offSet;
        int[] directions = MoveDirections.getDirections(getPosition());
        for(int index = 4; index < 8; index++){ // diagonals offsets start at index 4 to 7
            offSet = MoveDirections.directionOffSets[index];
            for(int i = 0; i < directions[index]; i++){
                end = getPosition() + (offSet * (i + 1));
                list.add(MoveGenerator.generateMove(getPosition(), end, 0));
                if(super.board.getTile(end).isOccupied()){
                    break;  // if piece is blocked, break from loop
                }
            }
        }
        return list;
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
