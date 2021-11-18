import java.util.ArrayList;

public class Bishop extends Piece{

    /*
     * Move Rules:
     * 1) Bishop only moves/attacks diagonally (top right, bot right, top left, bot left)
     *
     * Bishop moves by sliding. Check if piece is blocked by any piece. If piece is enemy, attack is possible.
     * Similar implementation to a Rook
     */

    public static final int BISHOP_MG_VALUE = 365, BISHOP_EG_VALUE = 297;

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
        for(int index = 4; index < 8; index++){ // diagonals offset start at index 4 to 7
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
    public int getExtraEval(){
        int positionBonus = 0;

        // bishop on large diagonals (2 diagonals that go from edge to edge) have higher mobility and is able to control the center
        if(getPosition() % 9 == 0 || getPosition() % 7 == 0){
            positionBonus += 74;
        }
        return positionBonus;
    }

    @Override
    public int getPieceValue(){
        return BISHOP_MG_VALUE;
    }

    @Override
    public int getMidGameValue(){
        int positionBonus = (isWhite()) ? EvalUtilities.bishopMidGamePST[getPosition()] : EvalUtilities.bishopMidGamePST[EvalUtilities.blackFlippedPosition[getPosition()]];
        return BISHOP_MG_VALUE + positionBonus;
    }

    @Override
    public int getEndGameValue(){
        int positionBonus = (isWhite()) ? EvalUtilities.bishopEndGamePST[getPosition()] : EvalUtilities.bishopEndGamePST[EvalUtilities.blackFlippedPosition[getPosition()]];
        return BISHOP_EG_VALUE + positionBonus;
    }

    @Override
    public int getPhaseValue(){
        return BISHOP_MG_VALUE;
    }

    @Override
    public String toString(){
        return "B";
    }
}
