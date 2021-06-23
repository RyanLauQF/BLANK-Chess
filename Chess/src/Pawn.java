import java.util.ArrayList;

public class Pawn extends Piece {

    /*
     * Move Rules:
     * 1) If pawn has not been move from starting position, it can move 2 tiles forward
     * 2) Pawn moves 1 tile forward in a regular move
     * 3) Pawn can move diagonally to attack
     * 4) En passant move   TODO
     * 5) If pawn reaches the opposite end, it can promote (Bishop, Knight, Rook, Queen)
     *
     * Pawn moves by sliding. Check if the pawn is blocked by any piece when it moves.
     */

    private static final int PAWN_VALUE = 1;
    private static final int[] whiteDirection = {-7, -8, -9};
    private static final int[] blackDirection = {7, 8, 9};

    public Pawn(boolean isWhite, int position, Board b){
        super(isWhite, position, b);  // call super class (parent class is Piece) constructor
    }

    @Override
    public ArrayList<Integer> getDefendingSquares(){
        // TODO
        ArrayList<Integer> list = new ArrayList<>();
        if(this.isWhite()){ // white moves
            if(this.getPosition() >= 48 && this.getPosition() <= 55){   // white pawn at starting position
                if(isValidMove(this.getPosition(), this.getPosition() - 16)){   // check if move is legal
                    list.add(this.getPosition() - 16); // pawn moves 2 steps forward
                }
            }
            // check through moving 1 tile forward and attacking tiles
            for(int i = 0; i < 3; i++){
                if(isValidMove(this.getPosition(), this.getPosition() + whiteDirection[i])){
                    list.add(this.getPosition() + whiteDirection[i]);
                }
            }
        }
        else{   // black moves
            if(this.getPosition() >= 8 && this.getPosition() <= 15){   // black pawn at starting position
                if(isValidMove(this.getPosition(), this.getPosition() + 16)){
                    list.add(this.getPosition() + 16); // pawn moves 2 steps forward
                }
            }
            // check through moving 1 tile forward and attacking tiles
            for(int i = 0; i < 3; i++){
                if(isValidMove(this.getPosition(), this.getPosition() + blackDirection[i])){
                    list.add(this.getPosition() + blackDirection[i]);
                }
            }
        }
        return list;
    }

    @Override
    public boolean isValidMove(int start, int end) {  // check if it is blocked
        if(start < 0 || start > 63 || end < 0 || end > 63){
            return false;   // out of bounds
        }
        // check if any piece is blocking the movement (for both 1 and 2 tile movements)
        if(Math.abs(end - start) ==  8){ // move 1 tile
            return !super.board.getTile(end).isOccupied();  // if tile in front is occupied by anything
        }
        else if(Math.abs(end - start) ==  16){  // check both tile not occupied
            return !super.board.getTile((start + end)/2).isOccupied() && !super.board.getTile(end).isOccupied();
        }
        // check diagonal attack moves
        // special cases: pawns on edge of board
        else if((Math.abs(start - end) == 7 || Math.abs(start - end) == 9) && super.board.getTile(end).isOccupied()){
            if(start % 8 == 0){ // pawn on left edge
                return Math.abs(start - end) != 9; // return false if it tries to attack left square
            }
            else if(start % 8 == 7 ){   // pawn on right edge
                return Math.abs(start - end) != 7; // return false if it tries to attack right square
            }
            return true;    // otherwise, return true
        }
        return false;
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
