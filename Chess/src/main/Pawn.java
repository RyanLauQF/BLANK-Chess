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

    private static final int PAWN_VALUE = 100;


    public Pawn(boolean isWhite, int position, Board b){
        super(isWhite, position, b);  // call super class (parent class is Piece) constructor
        this.type = PieceType.PAWN;
    }

    @Override
    public ArrayList<Short> getPossibleMoves(){
        ArrayList<Short> list = new ArrayList<>();
        int[] pawnMoveDirections = getPawnDirections(this.isWhite(), getPosition());
        generatePawnAttackMoves(pawnMoveDirections, this, list);
        generatePawnPushMoves(pawnMoveDirections[0], this, list);
        return list;
    }

    public static void generatePawnAttackMoves(int[] pawnDirections, Piece pawn, ArrayList<Short> list){
        int endPosition;
        for(int moves = 1; moves < pawnDirections.length; moves++){
            endPosition = pawnDirections[moves];
            if(isValidAttackingMove(pawn, pawn.getPosition(), endPosition)){
                int moveType = 4;   // standard capture move type
                if(endPosition == pawn.board.getEnpassant()){
                    moveType = 5;   // enpassant move type
                }
                list.add(MoveGenerator.generateMove(pawn.getPosition(), endPosition, moveType));
            }
        }
    }

    public static void generatePawnPushMoves(int pushDirection, Piece pawn, ArrayList<Short> list){
        boolean isWhitePawn = pawn.isWhite();
        int pawnPosition = pawn.getPosition();
        int endPosition;
        if(isValidSinglePawnPush(pawn, pawnPosition, pushDirection)){
            // index 0 is always single pawn push move
            list.add(MoveGenerator.generateMove(pawnPosition, pushDirection, 0));
        }
        if(canDoublePush(isWhitePawn, pawnPosition)){
            if(isWhitePawn){
                endPosition = pawnPosition - 16;
            }
            else{
                endPosition = pawnPosition + 16;
            }
            if(isValidDoublePawnPush(pawn, pawnPosition, endPosition)){
                list.add(MoveGenerator.generateMove(pawnPosition, endPosition, 1));
            }
        }
    }

    public static int[] getPawnDirections(boolean isWhite, int position){
        if(isWhite){
            return MoveDirections.getWhitePawnDirections(position);
        }
        else{
            return MoveDirections.getBlackPawnDirections(position);
        }
    }

    private static boolean isValidAttackingMove(Piece pawn, int start, int end) {  // check if it is blocked
        if(Math.abs(start - end) % 2 == 1){ // is an attacking move (as attacking moves index by 7 or 9 which is odd)
            // check if end position has an enemy piece
            // attacking move is also valid if it is on an enpassant square
            Tile tile = pawn.board.getTile(end);
            return (tile.isOccupied() && (tile.getPiece().isWhite() != pawn.isWhite()))
                    || (!tile.isOccupied() && pawn.board.getEnpassant() == end);
        }
        return false;
    }

    private static boolean isValidSinglePawnPush(Piece pawn, int start, int end){
        if(Math.abs(start - end) == 8){
            return !pawn.board.getTile(end).isOccupied();
        }
        return false;
    }

    private static boolean isValidDoublePawnPush(Piece pawn, int start, int end){
        if(end < 0 || end > 63){
            return false;
        }
        if(Math.abs(start - end) == 16){
            int middle = (start + end) / 2;
            return !pawn.board.getTile(end).isOccupied() && !pawn.board.getTile(middle).isOccupied();
        }
        return false;
    }

    /**
     * Checks pawn is at starting position, if a pawn is at starting position, it is able to double push
     * @return true if pawn is at start
     */
    private static boolean canDoublePush(boolean isWhite, int position){
        if(isWhite){
            return getRow(position) == 6;
        }
        else{
            return getRow(position) == 1;
        }
    }

    /**
     * Checks if a pawn is able to promote after making a move
     * @return true if a pawn of either side reaches the opposite side of the board
     */
    public static boolean canPromote(boolean isWhitePawn, int endPosition){
        if(isWhitePawn){
            return getRow(endPosition) == 0;
        }
        else{
            return getRow(endPosition) == 7;
        }
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
