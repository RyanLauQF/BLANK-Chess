/*
 *     Static class used for all FEN related methods to convert Chess FEN notation to board information
 *     Copyright (C) 2021 Ryan Lau Q. F.
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

/**
 * @author Ryan Lau Q. F.
 */
public class FENUtilities {

    // starting FEN notation for a normal game
    public static final String startFEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
    public static final String trickyFEN = "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1";

    /**
     * Converts information from FEN (Forsyth–Edwards Notation) to chess board
     *
     * The FEN String is parsed into 6 different sections based on the " " spacing
     * String[] splitFEN = FEN.split(" ");
     *
     *          splitFEN[0] contains piece position information
     *          splitFEN[1] contains turn information
     *          splitFEN[2] contains castling information
     *          splitFEN[3] contains enpassant information
     *          splitFEN[4] contains half move clock
     *          splitFEN[5] contains full move number
     *
     * @param FEN refers to the standard chess notation to describe a particular chess board position
     * @param b refers to the chess board
     */
    public static void convertFENtoBoard(String FEN, Board b){
        if(FEN == null ||  b == null){  // check null inputs
            throw new IllegalArgumentException("Null input");
        }
        Tile[] board = b.getBoard();

        // parse FEN string by " " to process different sections
        String[] splitFEN = FEN.split(" ");
        if(splitFEN.length != 6){ // minimum must have 6 sections
            throw new IllegalArgumentException("Not a valid FEN input");
        }

        // convert FEN piece information to board
        int currTile = 0;
        for(int i = 0; i < splitFEN[0].length(); i++){
            char c = splitFEN[0].charAt(i);
            if(c != '/'){   // ignore all slashes
                if (Character.isDigit(c)){ // a digit in FEN refers to number of empty tiles
                    for(int k = 0; k < Character.getNumericValue(c); k++){  // number of empty tiles
                        // create empty tiles on board
                        board[currTile] = new Tile(null);
                        currTile++;
                    }
                }
                // White pieces use upper-case letters ("PNBRQK")
                // Black pieces use lowercase letters ("pnbrqk")
                else{
                    // White Pieces
                    if(Character.isUpperCase(c)) {
                        b.getWhitePieces().addPiece(currTile);

                        if(c == 'P') board[currTile] = new Tile(new Pawn(true, currTile, b));
                        else if(c == 'N') board[currTile] = new Tile(new Knight(true, currTile, b));
                        else if(c == 'B') board[currTile] = new Tile(new Bishop(true, currTile, b));
                        else if(c == 'R') board[currTile] = new Tile(new Rook(true, currTile, b));
                        else if(c == 'Q') board[currTile] = new Tile(new Queen(true, currTile, b));
                        else if(c == 'K') board[currTile] = new Tile(new King(true, currTile, b));
                    }
                    // Black pieces
                    if(Character.isLowerCase(c)) {
                        b.getBlackPieces().addPiece(currTile);

                        if (c == 'p') board[currTile] = new Tile(new Pawn(false, currTile, b));
                        else if(c == 'n') board[currTile] = new Tile(new Knight(false, currTile, b));
                        else if(c == 'b') board[currTile] = new Tile(new Bishop(false, currTile, b));
                        else if(c == 'r') board[currTile] = new Tile(new Rook(false, currTile, b));
                        else if(c == 'q') board[currTile] = new Tile(new Queen(false, currTile, b));
                        else if(c == 'k') board[currTile] = new Tile(new King(false, currTile, b));
                    }
                    currTile++;
                }
            }
        }

        // process which side has current turn
        b.setTurn(splitFEN[1].equals("w"));

        // process castling data
        // initiate castling data to all false
        b.setWhiteKingSideCastle(false);
        b.setWhiteQueenSideCastle(false);
        b.setBlackKingSideCastle(false);
        b.setBlackQueenSideCastle(false);
        if(splitFEN[2].charAt(0) != '-'){   // check if castling is available for either side
            for(int i = 0; i < splitFEN[2].length(); i++){
                char c = splitFEN[2].charAt(i);
                if(c == 'K') b.setWhiteKingSideCastle(true);
                else if(c == 'Q') b.setWhiteQueenSideCastle(true);
                else if(c == 'k') b.setBlackKingSideCastle(true);
                else if(c == 'q') b.setBlackQueenSideCastle(true);
            }
        }

        // process enpassant availability data
        b.setEnpassant(-1); // initiate to -1 if not available
        if(splitFEN[3].charAt(0) != '-'){   // check if there is an enpassant move
            b.setEnpassant(convertRankAndFileToPosition(splitFEN[3]));
        }

        // process move count data
        b.setHalfMoveClock(Integer.parseInt(splitFEN[4]));  // set half move clock based on 50 move rule
        b.setFullMoveNum(Integer.parseInt(splitFEN[5]));    // set turn count
    }

    /**
     * Converts a rank and file format of chess position to and index from 0 to 63.
     * Board is indexed with zero starting at the top left and 63 being at the bottom right
     *
     * E.g. for a square at rank "a" and file "8", "a8" will be equal to index 0
     *      for a square at rank "h" and file "1", "h1" will be equal to index 63.
     *
     * @param rankAndFile refers to the standard rank and file position of a square on the board
     * @return the position on the board based on the rank and file of a square
     */
    public static int convertRankAndFileToPosition(String rankAndFile){
        if(rankAndFile.length() != 2 || !Character.isLetter(rankAndFile.charAt(0)) || !Character.isDigit(rankAndFile.charAt(1))){
            throw new IllegalArgumentException("Not a valid rank and file");
        }
        char file = rankAndFile.charAt(0);  // the letter
        int rank = Character.getNumericValue(rankAndFile.charAt(1));  // the number following the letter

        int BOARD_LENGTH = 8;   // length of chess board
        int ASCII_OF_a = 'a';   // ASCII of 'a', the first file of the board

        // since file 'a' is at column 0 and file 'b' is at column 1 etc.
        // to get column, take (char - ASCII_OF_a)
        return (Math.abs(rank - BOARD_LENGTH) * 8) + (file - ASCII_OF_a);
    }

    public static String convertIndexToRankAndFile(int index){
        int row = getRow(index);
        int col = getCol(index);

        char file = (char) ('a' + col);
        int rank = 8 - row;
        return String.valueOf(file) + rank;
    }

    private static int getRow(int position){
        return (position - (position % 8)) / 8;
    }

    private static int getCol(int position){
        return position % 8;
    }
}


