import java.util.ArrayList;

public class King extends Piece {

    /**
     * Move Rules:
     * 1) King can move/attack in all directions (diagonally, horizontally and vertically) by only 1 tile
     * 2) Castling Rules:
     *      - The king has not previously moved;
     *      - The chosen rook has not previously moved;
     *      - There must be no pieces between the king and the chosen rook;
     *      - The king is not currently in check;
     *      - Your king must not pass through a square that is under attack by enemy pieces;
     *      - The king must not end up in check.
     *
     * King moves by jumping. Check if piece is blocked by any piece. If piece is enemy, attack is possible.
     * Similar implementation to a knight
     */

    private static final int KING_VALUE = 20000;

    public King(boolean isWhite, int position, Board b){
        super(isWhite, position, b);
        b.setKingPosition(position, isWhite);
        this.type = PieceType.KING;
    }

    @Override
    // get all squares which piece is defending
    public ArrayList<Short> getPossibleMoves(){
        ArrayList<Short> list = new ArrayList<>();
        int endPosition, offSet;
        int[] directions = MoveDirections.getDirections(getPosition());
        for(int index = 0; index < 8; index++){
            offSet = MoveDirections.directionOffSets[index];
            for(int i = 0; i < directions[index] && i < 1; i++){
                endPosition = getPosition() + offSet;
                if(super.board.getTile(endPosition).isOccupied()){
                    if(super.board.getTile(endPosition).getPiece().isWhite() != this.isWhite()){
                        // capture
                        list.add(MoveGenerator.generateMove(getPosition(), endPosition, 4));
                    }
                    continue;
                }
                // Standard move with no capture
                list.add(MoveGenerator.generateMove(getPosition(), endPosition, 0));
            }
        }

        // check castling squares if the king is not under attack
        if(board.hasCastlingRights()){
            if(!super.board.isTileAttacked(getPosition(), this.isWhite())){
                if(board.hasKingSideCastling(this.isWhite())){
                    if(checkKingSideCastling()){
                        // king jumps 2 squares to the right for king side castling
                        endPosition = this.getPosition() + 2;
                        list.add(MoveGenerator.generateMove(getPosition(), endPosition, 2));
                    }
                }
                if(board.hasQueenSideCastling(this.isWhite())){
                    if(checkQueenSideCastling()){
                        // king jumps 2 squares to the left for queen side castling
                        endPosition = this.getPosition() - 2;
                        list.add(MoveGenerator.generateMove(getPosition(), endPosition, 3));
                    }
                }
            }
        }
        return list;
    }

    /**
     * Checks conditions for castling according to above-mentioned rules for king side castling
     * @return true if king side castling if available, else return false
     */
    public boolean checkKingSideCastling(){
        // check if the path between king and king side rook is blocked by any piece
        int rookPosition = this.getPosition() + 3;  // king side rook position
        // if king side rook is at position, check if path king is taking is clear to castle
        if(super.board.getTile(rookPosition).isOccupied() && super.board.getTile(rookPosition).getPiece().isRook()){
            return isPathClear(this.getPosition(), rookPosition, false);
        }
        return false;
    }

    /**
     * Checks conditions for castling according to above-mentioned rules for queen side castling
     * @return true if queen side castling if available, else return false
     */
    public boolean checkQueenSideCastling(){
        // check if the path between king and queen side rook is blocked by any piece
        int rookPosition = this.getPosition() - 4;  // queen side rook position
        // if king side rook is at position, check if path king is taking is clear to castle
        if(super.board.getTile(rookPosition).isOccupied() && super.board.getTile(rookPosition).getPiece().isRook()){
            return isPathClear(this.getPosition(), rookPosition, true);
        }
        return false;
    }

    /**
     * Checks path between rook and king is occupied
     * Checks if path king is taking is being attacked
     *
     * E.g.      0 | 1 | 2 | 3
     *   ->  Start | 1 | 2 | End
     * if king is on tile 0 and rook is on tile 3, check if tile 1 and 2 are clear
     *
     * @param startPosition refers to the start position on the board
     * @param endPosition refers to the end position on the board
     * @return true if path between king and rook is cleared for castling, else return false
     */
    private boolean isPathClear(int startPosition, int endPosition, boolean isQueenSideCastling){
        // If queen side castling, check the tile directly beside rook if it is occupied as even if the tile
        // is under attack king can still castle
        if(isQueenSideCastling){
            endPosition++;
            if(super.board.getTile(endPosition).isOccupied()){
                return false;
            }
        }
        int diff = Math.abs(startPosition - endPosition);
        for(int i = 1; i < diff; i++){
            if(startPosition < endPosition){
                if(super.board.getTile(startPosition + i).isOccupied()) return false;
                if(super.board.isTileAttacked(startPosition + i, isWhite())) return false;
            }
            else{
                if(super.board.getTile(startPosition - i).isOccupied()) return false;
                if(super.board.isTileAttacked(startPosition - i, isWhite())) return false;
            }
        }
        return true;
    }

    @Override
    public int getValue(){  // value of a king
        return KING_VALUE;
    }

    @Override
    public String toString(){
        return "K";
    }
}
