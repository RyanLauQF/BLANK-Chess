import java.util.ArrayList;

public class Bishop extends Piece{

    /*
     * Move Rules:
     * 1) Bishop only moves/attacks diagonally (top right, bot right, top left, bot left)
     *
     * Bishop moves by sliding. Check if piece is blocked by any piece. If piece is enemy, attack is possible.
     * Similar implementation to a Rook
     */

    private static final int BISHOP_VALUE = 330;

    public Bishop(boolean isWhite, int position, Board b){
        super(isWhite, position, b);
        this.type = PieceType.BISHOP;
    }

    @Override
    public ArrayList<Short> getPossibleMoves(){
        ArrayList<Short> list = new ArrayList<>();
        // check each diagonal path until it reaches edge of board or get blocked by another piece
        int end, offSet;
        Tile endTile;
        int[] directions = MoveDirections.getDirections(getPosition());
        for(int index = 4; index < 8; index++){ // diagonals offsets start at index 4 to 7
            offSet = MoveDirections.directionOffSets[index];
            for(int i = 0; i < directions[index]; i++){
                end = getPosition() + (offSet * (i + 1));
                endTile = super.board.getTile(end);
                if(endTile.isOccupied()){
                    if(endTile.getPiece().isWhite() != this.isWhite()){
                        // enemy piece capture
                        list.add(MoveGenerator.generateMove(getPosition(), end, 4));
                    }
                    break;
                }
                else{
                    // standard movement with no capture
                    list.add(MoveGenerator.generateMove(getPosition(), end, 0));
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
