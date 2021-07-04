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

    private static final int KING_VALUE = 100;

    public King(boolean isWhite, int position, Board b){
        super(isWhite, position, b);
        b.setKingPosition(position, isWhite);
        this.type = PieceType.KING;
    }

    @Override
    // get all squares which piece is defending
    public ArrayList<Short> getDefendingSquares(){
        ArrayList<Short> list = new ArrayList<>();
        int endPosition, offSet;
        int[] directions = MoveDirections.getDirections(getPosition());
        for(int index = 0; index < 8; index++){
            offSet = MoveDirections.directionOffSets[index];
            for(int i = 0; i < directions[index] && i < 1; i++){
                endPosition = getPosition() + offSet;
                list.add(MoveGenerator.generateMove(getPosition(), endPosition, 0));
            }
        }

        // check castling squares
        if(checkKingSideCastling()){
            // king jumps 2 squares to the right for king side castling
            endPosition = this.getPosition() + 2;
            list.add(MoveGenerator.generateMove(getPosition(), endPosition, 0));
        }
        else if(checkQueenSideCastling()){
            // king jumps 2 squares to the left for queen side castling
            endPosition = this.getPosition() - 2;
            list.add(MoveGenerator.generateMove(getPosition(), endPosition, 0));
        }
        return list;
    }

    /**
     * Checks conditions for castling according to above-mentioned rules for king side castling
     * @return true if king side castling if available, else return false
     */
    public boolean checkKingSideCastling(){
        if(super.board.kingCheckedCount(this.isWhite()) != 0){    // cannot castle when king is in check
            return false;
        }
        if(this.isWhite() && !super.board.getWhiteKingSideCastle()){ // check if white has king side castling rights
            return false;
        }
        else if(!this.isWhite() && !super.board.getBlackKingSideCastle()){  // check if black has king side castling rights
            return false;
        }
        // check if the path between king and king side rook is blocked by any piece
        int rookPosition;   // king side rook position
        rookPosition = this.getPosition() + 3;
        // if king side rook is at position check if path is clear to castle
        if(super.board.getTile(rookPosition).isOccupied() && super.board.getTile(rookPosition).getPiece().isRook()){
            return isPathClear(this.getPosition(), rookPosition);
        }
        return false;
    }

    /**
     * Checks conditions for castling according to above-mentioned rules for queen side castling
     * @return true if queen side castling if available, else return false
     */
    public boolean checkQueenSideCastling(){
        if(super.board.kingCheckedCount(this.isWhite()) != 0){    // cannot castle when king is in check
            return false;
        }
        if(this.isWhite() && !super.board.getWhiteQueenSideCastle()){ // check if white has king side castling rights
            return false;
        }
        else if(!this.isWhite() && !super.board.getBlackQueenSideCastle()){  // check if black has king side castling rights
            return false;
        }
        // check if the path between king and queen side rook is blocked by any piece
        // queen side rook position
        int rookPosition = this.getPosition() - 4;
        // if king side rook is at position check if path is clear to castle
        if(super.board.getTile(rookPosition).isOccupied() && super.board.getTile(rookPosition).getPiece().isRook()){
            return isPathClear(this.getPosition(), rookPosition);
        }
        return false;
    }

    /**
     * Checks if the tiles between start and end are clear tiles
     * Clear tiles refer to tiles that are not being occupied or attacked
     *
     * E.g.     0 | 1 | 2 | 3
     *   ->  King | 1 | 2 | Rook
     * if king is on tile 0 and rook is on tile 3, check if tile 1 and 2 are clear
     *
     * @param kingPosition refers to the king position on the board
     * @param rookPosition refers to the rook position on the board
     * @return true if path between king and rook is cleared for castling, else return false
     */
    private boolean isPathClear(int kingPosition, int rookPosition){
        int diff = Math.abs(kingPosition - rookPosition);
        for(int i = 1; i < diff; i++){
            if(kingPosition < rookPosition){
                if(super.board.getTile(kingPosition + i).isOccupied()) return false;
                if(super.board.isTileAttacked(kingPosition + i, isWhite())) return false;
            }
            else{
                if(super.board.getTile(kingPosition - i).isOccupied()) return false;
                if(super.board.isTileAttacked(kingPosition - i, isWhite())) return false;
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
