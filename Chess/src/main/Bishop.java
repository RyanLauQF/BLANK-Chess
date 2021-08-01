import java.util.ArrayList;

public class Bishop extends Piece{

    /*
     * Move Rules:
     * 1) Bishop only moves/attacks diagonally (top right, bot right, top left, bot left)
     *
     * Bishop moves by sliding. Check if piece is blocked by any piece. If piece is enemy, attack is possible.
     * Similar implementation to a Rook
     */

    public static final int BISHOP_VALUE = 374;

    public Bishop(boolean isWhite, int position, Board b){
        super(isWhite, position, b);
        this.type = PieceType.BISHOP;
    }

    @Override
    public ArrayList<Short> getPossibleMoves(boolean generateCapturesOnly){
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
                    if(!generateCapturesOnly){  // disable normal moves
                        // standard movement with no capture
                        list.add(MoveGenerator.generateMove(getPosition(), end, 0));
                    }
                }
            }
        }
        return list;
    }

    @Override
    public int getValue(){  // value of a bishop
        int positionBonus = (isWhite()) ? EvalUtilities.bishopPST[getPosition()] : EvalUtilities.bishopPST[EvalUtilities.blackFlippedPosition[getPosition()]];

        // bishop on large diagonals (2 diagonals that go from edge to edge) have higher mobility and is able to control the center
        if(getPosition() % 9 == 0 || getPosition() % 7 == 0){
            positionBonus += 74;
        }

        return BISHOP_VALUE + positionBonus;
    }

    @Override
    public int getPieceValue(){
        return BISHOP_VALUE;
    }

    @Override
    public String toString(){
        return "B";
    }
}
