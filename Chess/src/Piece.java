import java.util.ArrayList;

public abstract class Piece {
    protected final boolean colour; // White == 1, Black == 0;
    protected int position; // position of piece on board
    protected Board board; // reference to the chess board

    // constructor
    public Piece(boolean isWhite, int position, Board board){
        if(position < 0 || position > 63 || board == null){
            throw new IllegalArgumentException();
        }
        this.position = position;
        this.colour = isWhite;
        this.board = board;
    }

    // returns all legal moves of a piece
    public ArrayList<Integer> getLegalMoves(){
        ArrayList<Integer> moveList = new ArrayList<>();
        ArrayList<Integer> defendingSquares = getDefendingSquares();

        for(int move : defendingSquares){
            if(board.getTile(move).isOccupied()){   // check if piece is attacking / defending another piece
                // filter out allied pieces as it cannot move to tiles occupied by allied pieces
                if(board.getTile(move).getPiece().isWhite() == this.isWhite()) {
                    continue;
                }
            }
            /*
             *  At this point, the move will become a pseudo-legal move.
             *  test the pseudo-legal move to check if king is under check after moving any piece
             */

            Move movement = new Move(this.board, this.position, move);
            movement.makeMove();    // make the move on the board without making a copy
            // if king is not under check after making the move, the move is legal.
            if(!this.board.isKingChecked()) {
                moveList.add(move);
            }
            movement.unMake();  // revert board back to its original state
        }
        return moveList;
    }

    public final boolean isWhite(){
        return colour;
    }

    public final int getPosition() {
        return position;
    }

    public final void setPosition(int position){
        this.position = position;
    }

    public int getRow(int position){
        return (position - (position % 8)) / 8;
    }

    public int getCol(int position){
        return position % 8;
    }

    /***** TO BE IMPLEMENTED BY SUB CLASSES *****/

    // returns a list of all defending squares of a piece (squares which piece controls)
    public abstract ArrayList<Integer> getDefendingSquares();

    // checks if a move is within boundaries and possible (regardless if there is a piece at end position)
    public abstract boolean isValidMove(int start, int end);

    // gets abbreviation of piece name
    public abstract String toString();

    // returns value of a piece
    public abstract int getValue();
}
