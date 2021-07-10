import java.util.ArrayList;

public class Pawn extends Piece {

    /*
     * Move Rules:
     * 1) If pawn has not been move from starting position, it can move 2 tiles forward
     * 2) Pawn moves 1 tile forward in a regular move
     * 3) Pawn can move diagonally to attack
     * 4) En passant move
     * 5) If pawn reaches the opposite end, it can promote (Bishop, Knight, Rook, Queen)
     *
     * Pawn moves by sliding. Check if the pawn is blocked by any piece when it moves.
     */

    private static final int PAWN_VALUE = 1;
    private final int START_POSITION;


    public Pawn(boolean isWhite, int position, Board b){
        super(isWhite, position, b);  // call super class (parent class is Piece) constructor
        this.type = PieceType.PAWN;
        this.START_POSITION = position;
    }

    @Override
    public ArrayList<Short> getDefendingSquares(){
        ArrayList<Short> list = new ArrayList<>();
        int endPosition;
        if(this.isWhite()){
            int[] whitePawnDirections = MoveDirections.getWhitePawnDirections(getPosition());
            for (int moves = 0; moves < whitePawnDirections.length; moves++) {
                endPosition = whitePawnDirections[moves];
                if(moves == 0 && isValidSinglePawnPush(getPosition(), endPosition)){
                    // index 0 is always single pawn push move
                    list.add(MoveGenerator.generateMove(getPosition(), endPosition, 0));
                }
                else if(isValidAttackingMove(getPosition(), endPosition)){
                    int moveType = 4;   // capture move type
                    if(endPosition == board.getEnpassant()){
                        moveType = 5;   // enpassant move type
                    }
                    list.add(MoveGenerator.generateMove(getPosition(), endPosition, moveType));
                }
            }
            endPosition = getPosition() - 16;
            if(canDoublePush() && isValidDoublePawnPush(getPosition(), endPosition)){
                list.add(MoveGenerator.generateMove(getPosition(), endPosition, 1));
            }
        }
        else{
            int[] blackPawnDirections = MoveDirections.getBlackPawnDirections(getPosition());
            for (int moves = 0; moves < blackPawnDirections.length; moves++) {
                endPosition = blackPawnDirections[moves];
                if(moves == 0 && isValidSinglePawnPush(getPosition(), endPosition)){
                    // index 0 is always single pawn push move
                    list.add(MoveGenerator.generateMove(getPosition(), endPosition, 0));
                }
                else if(isValidAttackingMove(getPosition(), endPosition)){
                    int moveType = 4;   // capture move type
                    if(endPosition == board.getEnpassant()){
                        moveType = 5;   // enpassant move type
                    }
                    list.add(MoveGenerator.generateMove(getPosition(), endPosition, moveType));
                }
            }
            endPosition = getPosition() + 16;
            if(canDoublePush() && isValidDoublePawnPush(getPosition(), endPosition)){
                list.add(MoveGenerator.generateMove(getPosition(), endPosition, 1));
            }
        }
        return list;
    }


    private boolean isValidAttackingMove(int start, int end) {  // check if it is blocked
        if(Math.abs(start - end) % 2 == 1){ // is an attacking move (as attacking moves index by 7 or 9 which is odd)
            // check if end position has an enemy piece
            // attacking move is also valid if it is on an enpassant square
            Tile tile = super.board.getTile(end);
            return (tile.isOccupied() && (tile.getPiece().isWhite() != this.isWhite()))
                    || (!tile.isOccupied() && super.board.getEnpassant() == end);
        }
        return false;
    }

    private boolean isValidSinglePawnPush(int start, int end){
        if(Math.abs(start - end) == 8){
            return !super.board.getTile(end).isOccupied();
        }
        return false;
    }

    private boolean isValidDoublePawnPush(int start, int end){
        if(end < 0 || end > 63){
            return false;
        }
        if(Math.abs(start - end) == 16){
            int middle = (start + end) / 2;
            return !super.board.getTile(end).isOccupied() && !super.board.getTile(middle).isOccupied();
        }
        return false;
    }

    private boolean canDoublePush(){
        return this.getPosition() == START_POSITION;
    }

    @Override
    public int getValue(){  // value of a pawn
        return PAWN_VALUE;
    }

    @Override
    public String toString(){
        return "P";
    }
}
