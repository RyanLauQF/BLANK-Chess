public class FENUtilities { // static class for all FEN related methods

    // starting FEN notation for a normal game
    public static final String startFEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    public FENUtilities(){
        throw new IllegalArgumentException("Do Not Instantiate");
    }

    // converts information from FEN to chess board
    public static void convertFENtoBoard(String FEN, Board b){
        if(FEN == null ||  b == null){  // check null inputs
            throw new IllegalArgumentException("Null input");
        }
        Tile[] board = b.getBoard();

        // parse FEN string by " " to process different sections
        String[] splitFEN = FEN.split(" ");

        /*
         * splitFEN[0] contains piece position information
         * splitFEN[1] contains turn information
         * splitFEN[2] contains castling information
         * splitFEN[3] contains En passant information
         * splitFEN[4] contains half move clock
         * splitFEN[5] contains full move number
        */

        if(splitFEN.length != 6){ // minimum must have 6 sections
            throw new IllegalArgumentException("Not a valid FEN input");
        }

        // convert FEN piece information to board
        int currTile = 0;
        for(int i = 0; i < splitFEN[0].length(); i++){
            char c = FEN.charAt(i);
            if(c != '/'){   // ignore all slashes
                if (Character.isDigit(c)){ // a digit in FEN refers to number of empty tiles
                    for(int k = 0; k < Character.getNumericValue(c); k++){  // number of empty tiles
                        // create empty tiles on board
                        board[currTile] = new Tile(null, currTile);
                        currTile++;
                    }
                }
                // White pieces use upper-case letters ("PNBRQK")
                // Black pieces use lowercase letters ("pnbrqk")
                else{
                    // White Pieces
                    if(Character.isUpperCase(c)) {
                        b.getWhitePieces().add(currTile);

                        if(c == 'P') board[currTile] = new Tile(new Pawn(true, currTile, b), currTile);
                        else if(c == 'N') board[currTile] = new Tile(new Knight(true, currTile, b), currTile);
                        else if(c == 'B') board[currTile] = new Tile(new Bishop(true, currTile, b), currTile);
                        else if(c == 'R') board[currTile] = new Tile(new Rook(true, currTile, b), currTile);
                        else if(c == 'Q') board[currTile] = new Tile(new Queen(true, currTile, b), currTile);
                        else if(c == 'K') board[currTile] = new Tile(new King(true, currTile, b), currTile);
                    }
                    // Black pieces
                    if(Character.isLowerCase(c)) {
                        b.getBlackPieces().add(currTile);

                        if (c == 'p') board[currTile] = new Tile(new Pawn(false, currTile, b), currTile);
                        else if(c == 'n') board[currTile] = new Tile(new Knight(false, currTile, b), currTile);
                        else if(c == 'b') board[currTile] = new Tile(new Bishop(false, currTile, b), currTile);
                        else if(c == 'r') board[currTile] = new Tile(new Rook(false, currTile, b), currTile);
                        else if(c == 'q') board[currTile] = new Tile(new Queen(false, currTile, b), currTile);
                        else if(c == 'k') board[currTile] = new Tile(new King(false, currTile, b), currTile);
                    }
                    currTile++;
                }
            }
        }

        b.setTurn(splitFEN[1].equals("w")); // set which turn
//        b.setHalfMoveClock(Integer.parseInt(splitFEN[4]));  // set half move clock based on 50 move rule
//        b.setFullMoveNum(Integer.parseInt(splitFEN[5]));    // set turn count
    }
}
