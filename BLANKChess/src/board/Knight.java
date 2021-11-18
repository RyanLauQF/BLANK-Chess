import java.util.ArrayList;

public class Knight extends Piece{

    /*
     * Move Rules:
     * 1) Knight moves/attacks in an L shape
     * 2) Max number of moves is 8.
     *
     * Knight moves by jumping over other pieces. Check if piece landed on is an allied piece.
     */

    public static final int KNIGHT_MG_VALUE = 337, KNIGHT_EG_VALUE = 281;

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

    public int getKnightPeriphery(){
        int periphery;
        int position = this.getPosition();
        int row = getRow(position);
        int col = getCol(position);

        if(position >= 9 && position <= 14
            || position >= 49 && position <= 54
            || (col == 1 || col == 6) && (row >= 1 && row <= 6)){
            periphery = 1;
        }

        else if(position >= 18 && position <= 21
                || position >= 42 && position <= 45
                || (col == 2 || col == 5) && (row >= 2 && row <= 5)){
            periphery = 2;
        }

        else if(position == 27 || position == 28 || position == 35 || position == 36){
            periphery = 3;
        }
        else{
            periphery = 0;
        }
        return periphery;
    }

    @Override
    public int getExtraEval(){
        int positionBonus = 0;

        // bonus points for knight being defended by an allied pawn
        if(board.checkPawnAttacking(!isWhite(), getPosition(), 0) > 0){
            positionBonus += 40;
        }

//        int[] periBonus = {-51, -18, 45, -1};
//        positionBonus += periBonus[getKnightPeriphery()];

        return positionBonus;
    }

    @Override
    public int getPieceValue(){
        return KNIGHT_MG_VALUE;
    }

    @Override
    public int getMidGameValue(){
        int positionBonus = (isWhite()) ? EvalUtilities.knightMidGamePST[getPosition()] : EvalUtilities.knightMidGamePST[EvalUtilities.blackFlippedPosition[getPosition()]];
        return KNIGHT_MG_VALUE + positionBonus;
    }

    @Override
    public int getEndGameValue(){
        int positionBonus = (isWhite()) ? EvalUtilities.knightEndGamePST[getPosition()] : EvalUtilities.knightEndGamePST[EvalUtilities.blackFlippedPosition[getPosition()]];
        return KNIGHT_EG_VALUE + positionBonus;
    }

    @Override
    public int getPhaseValue(){
        return KNIGHT_MG_VALUE;
    }

    @Override
    public String toString(){
        return "N";
    }
}
