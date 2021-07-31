import java.util.ArrayList;

public class Queen extends Piece{

    /*
     * Move Rules:
     * 1) Queen can move/attack in all directions (diagonally, horizontally and vertically)
     *
     * Queen moves by sliding. Check if piece is blocked by any piece. If piece is enemy, attack is possible.
     * Similar implementation to a Bishop + Rook movement
     */

    public static final int QUEEN_VALUE = 911;

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
    public int getValue(){
        int positionBonus = (isWhite()) ? EvalUtilities.queenPST[getPosition()] : EvalUtilities.queenPST[EvalUtilities.blackFlippedPosition[getPosition()]];
        return QUEEN_VALUE + positionBonus;
    }

    @Override
    public String toString(){
        return "Q";
    }
}
