import java.util.ArrayList;

public class Queen extends Piece{

    /*
     * Move Rules:
     * 1) Queen can move/attack in all directions (diagonally, horizontally and vertically)
     *
     * Queen moves by sliding. Check if piece is blocked by any piece. If piece is enemy, attack is possible.
     * Similar implementation to a Bishop + Rook movement
     */

    public static final int QUEEN_MG_VALUE = 1025, QUEEN_EG_VALUE = 936;

    public Queen(boolean isWhite, int position, Board b){
        super(isWhite, position, b);
        this.type = PieceType.QUEEN;
    }

    @Override
    public ArrayList<Short> getPossibleMoves(boolean generateCapturesOnly){
        ArrayList<Short> list = new ArrayList<>();
        int end, offSet;
        Tile endTile;
        int[] directions = MoveDirections.getDirections(getPosition());
        for(int index = 0; index < 8; index++){
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
                    if(!generateCapturesOnly){  // disable quiet moves if generating captures
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
        return 0;
    }

    @Override
    public int getMidGameValue(){
        int positionBonus = (isWhite()) ? EvalUtilities.queenMidGamePST[getPosition()] : EvalUtilities.queenMidGamePST[EvalUtilities.blackFlippedPosition[getPosition()]];
        return QUEEN_MG_VALUE + positionBonus;
    }

    @Override
    public int getEndGameValue(){
        int positionBonus = (isWhite()) ? EvalUtilities.queenEndGamePST[getPosition()] : EvalUtilities.queenEndGamePST[EvalUtilities.blackFlippedPosition[getPosition()]];
        return QUEEN_EG_VALUE + positionBonus;
    }

    @Override
    public int getPhaseValue(){
        return QUEEN_MG_VALUE;
    }

    @Override
    public String toString(){
        return "Q";
    }
}
