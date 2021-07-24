import java.util.ArrayList;

public class Rook extends Piece{

    /*
     * Move Rules:
     * 1) Rook only moves/attacks vertically or horizontally (up, down, left, right)
     * 2) Rook can jump to castle with king
     *
     * Rook moves by sliding. Check if piece is blocked by any piece. If piece is enemy, attack is possible.
     * Similar implementation to a Bishop
     */

    private static final int ROOK_VALUE = 530;

    public Rook(boolean isWhite, int position, Board b){
        super(isWhite, position, b);
        this.type = PieceType.ROOK;
    }

    @Override
    public ArrayList<Short> getPossibleMoves(boolean generateCapturesOnly){
        ArrayList<Short> list = new ArrayList<>();
        int end, offSet;
        Tile endTile;
        int[] directions = MoveDirections.getDirections(getPosition());
        for(int index = 0; index < 4; index++){ // straight offsets start at index 0 to 3
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
                    if(!generateCapturesOnly){
                        // standard movement with no capture
                        list.add(MoveGenerator.generateMove(getPosition(), end, 0));
                    }
                }
            }
        }
        return list;
    }

    private boolean isSeventhRankRook(){
        if(isWhite()){
            return Piece.getRow(getPosition()) == 1;
        }
        else{
            return Piece.getRow(getPosition()) == 6;
        }
    }

    @Override
    public int getValue(){  // value of a rook
        int positionBonus = (isWhite()) ? EvalUtilities.rookPST[getPosition()] : EvalUtilities.rookPST[EvalUtilities.blackFlippedPosition[getPosition()]];

        if(isSeventhRankRook()){    // rook on rank 7 is able to create a major threat
            positionBonus += 41;
        }
        return ROOK_VALUE + positionBonus;
    }

    @Override
    public String toString(){
        return "R";
    }
}
