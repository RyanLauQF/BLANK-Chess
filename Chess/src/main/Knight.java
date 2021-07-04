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

    public Knight(boolean isWhite, int position, Board b){
        super(isWhite, position, b);
        this.type = PieceType.KNIGHT;
    }

    @Override
    public ArrayList<Short> getDefendingSquares(){
        ArrayList<Short> list = new ArrayList<>();
        int[] knightDirections = MoveDirections.getKnightDirections(getPosition());
        for (int knightDirection : knightDirections) {
            list.add(MoveGenerator.generateMove(getPosition(), knightDirection, 0));
        }

        return list;
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
