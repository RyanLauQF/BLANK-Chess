import java.util.ArrayList;

public class Knight extends Piece{

    /*
     * Move Rules:
     * 1) Knight moves/attacks in an L shape
     * 2) Max number of moves is 8.
     *
     * Knight moves by jumping over other pieces. Check if piece landed on is an allied piece.
     */

    private static final int KNIGHT_VALUE = 342;

    public Knight(boolean isWhite, int position, Board b){
        super(isWhite, position, b);
        this.type = PieceType.KNIGHT;
    }

    @Override
    public ArrayList<Short> getPossibleMoves(boolean generateCapturesOnly){
        ArrayList<Short> list = new ArrayList<>();
        int[] knightDirections = MoveDirections.getKnightDirections(getPosition());
        for (int knightDirection : knightDirections) {
            if(super.board.getTile(knightDirection).isOccupied()){
                if(super.board.getTile(knightDirection).getPiece().isWhite() != this.isWhite()){
                    // capture
                    list.add(MoveGenerator.generateMove(getPosition(), knightDirection, 4));
                }
                continue;
            }
            // Standard move with no capture
            if(!generateCapturesOnly){  // disable quiet moves
                list.add(MoveGenerator.generateMove(getPosition(), knightDirection, 0));
            }
        }

        return list;
    }

    @Override
    public int getValue(){  // value of a knight
        int positionBonus = (isWhite()) ? EvalUtilities.knightPST[getPosition()] : EvalUtilities.knightPST[EvalUtilities.blackFlippedPosition[getPosition()]];

        // bonus points for knight being defended by an allied pawn
        if(board.checkPawnAttacking(!isWhite(), getPosition(), 0) > 0){
            positionBonus += 40;
        }
        return KNIGHT_VALUE + positionBonus;
    }

    @Override
    public String toString(){
        return "N";
    }
}
