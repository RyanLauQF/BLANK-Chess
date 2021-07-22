import java.io.*;
import java.net.URL;

public class PGNExtract {
    /**
     * Takes a PGN file in the same directory and processes it to be used to create the opening trie
     * PGN file is created by exporting games from SCID database.
     *
     * How it is filtered:
     *      1) For each game, remove all extra information except for the moves
     *      2) Only keep moves up to move 8, all moves after will not be needed to build opening
     *      3) Remove all move numberings leaving only the moves itself.
     *
     *      *All games must start from standard notation to utilise opening trie
     *
     * @param fileName refers to the PGN file to be processed
     */
    public static void processPGN(String fileName) throws IOException {
        // processes the PGN files by creating a new PGN file containing only the moves inside
        URL path = PGNExtract.class.getResource(fileName);

        File file = new File(path.getFile());
        File newFile = new File("whiteProcessedBook.txt");

        BufferedReader reader = new BufferedReader(new FileReader(file));
        BufferedWriter writer = new BufferedWriter(new FileWriter(newFile));

        int index = 0;
        String currentLine;
        while((currentLine = reader.readLine()) != null){
            if(currentLine.equals("") || currentLine.charAt(0) == '[' || currentLine.charAt(0) != '1'){
                // ignore all empty lines and lines which start with '['.
                // checks if the line starts with 1. which represents move 1
                // information in brackets are not required
                continue;
            }
            // each line contains the moves for an entire game
            // process up to move 8. (everything after move 9 can be ignored)
            StringBuilder processedLines = new StringBuilder();
            for(int i = 0; i < currentLine.length(); i++){
                char ch = currentLine.charAt(i);
                if(Character.isDigit(ch)){
                    if(i + 1 < currentLine.length()){
                        int checkNext = i + 1;
                        // once reached the 9th move break and stop processing.
                        if(Character.getNumericValue(ch) == 9 && currentLine.charAt(checkNext) == '.'){
                            break;
                        }
                        else{
                            // filter out the numbers "1. 2. 3. ..." from each line to prepare them to be processed into the opening trie
                            if (currentLine.charAt(checkNext) == '.') {
                                i += 2;
                                continue;
                            }
                        }

                    }
                }
                processedLines.append(ch);
            }
            String processed = processedLines.toString();
            writer.write(processed + "\n");
            index++;
        }
        writer.flush();
        writer.close();
        reader.close();
        System.out.println("Processed " + index + " lines");
    }

    // converts the algebraic notation in PGN to an encoded move used by engine
    public static short convertNotationToMove(Board board, boolean isWhiteTurn, String notation){
        short encodedMove = 0;

        // cut away check(+) and checkmate(#) notations from the string
        if(notation.charAt(notation.length() - 1) == '+' || notation.charAt(notation.length() - 1) == '#'){
            notation = notation.substring(0, notation.length() - 1);
        }

        // castling moves
        if(notation.equals("O-O")){
            // king side castling
            int kingPosition = board.getKingPosition(isWhiteTurn);
            int endPosition = kingPosition + 2;
            return MoveGenerator.generateMove(kingPosition, endPosition, 2);
        }
        else if(notation.equals("O-O-O")){
            // queen side castling
            int kingPosition = board.getKingPosition(isWhiteTurn);
            int endPosition = kingPosition - 2;
            return MoveGenerator.generateMove(kingPosition, endPosition, 3);
        }

        // gets the attacking piece type
        Piece.PieceType movingPieceType = null;
        char startPiece = notation.charAt(0);
        // if the character at index 0 is a upper case, means it is not a pawn moving
        if(Character.isUpperCase(startPiece)){
            if(startPiece == 'N'){
                movingPieceType = Piece.PieceType.KNIGHT;
            }
            else if(startPiece == 'B'){
                movingPieceType = Piece.PieceType.BISHOP;
            }
            else if(startPiece == 'R'){
                movingPieceType = Piece.PieceType.ROOK;
            }
            else if(startPiece == 'Q'){
                movingPieceType = Piece.PieceType.QUEEN;
            }
            else if(startPiece == 'K'){
                movingPieceType = Piece.PieceType.KING;
            }
        }
        else{   // pawn move
            movingPieceType = Piece.PieceType.PAWN;
        }

        int notationLength = notation.length();
        boolean isCapture = (notation.indexOf('x') != -1);  // if it contains an 'x' means it is a capture move
        int moveType;

        PieceList pieces = board.getPieceList(isWhiteTurn);
        if(movingPieceType == Piece.PieceType.PAWN){    // process pawn moves first due to the special moves of pawn
            boolean isPromotion = (notation.indexOf('=') != -1);   // if it contains a '=' means it is a promotion move
            if(notationLength == 2){    // normal pawn push
                // find the pawn in file
                // get the pawn in file and move it
                char file = notation.charAt(0);
                char rank = notation.charAt(1);

                moveType = 0;   // standard 1 square pawn push

                if(isPromotion){    // pawn push and promotes
                    // the last character in string represents promotion type
                    char promotionType = notation.charAt(notation.indexOf('=') + 1);
                    if(promotionType == 'Q'){
                        // queen promotion
                        moveType = 11;
                    }
                    else if(promotionType == 'N'){
                        moveType = 8;
                    }
                    else if(promotionType == 'B'){
                        moveType = 9;
                    }
                    else if(promotionType == 'R'){
                        moveType = 10;
                    }
                }

                int endPosition = PGNExtract.convertFileAndRankToIndex(file, rank);
                for(int i = 0; i < pieces.getCount(); i++){
                    // find the pawn in the same file with a legal move that ends at end position
                    Piece piece = board.getTile(pieces.occupiedTiles[i]).getPiece();
                    if(piece.isPawn() && Piece.getFile(piece.getPosition()) == file){
                        for(Short legalMove : piece.getLegalMoves()){
                            if(MoveGenerator.getEnd(legalMove) == endPosition && !MoveGenerator.isCapture(legalMove)){
                                // if the move lands on the end position and is not a capture
                                int startPosition = piece.getPosition();
                                if(Math.abs(startPosition - endPosition) == 16){
                                    moveType = 1;   // double pawn push
                                }
                                return MoveGenerator.generateMove(startPosition, endPosition, moveType);
                            }
                        }
                    }
                }
            }
            else if(isCapture){
                // e.g. exd5 (pawn on the e-file captures the piece on d5).
                char startFile = notation.charAt(0);
                char endFile = notation.charAt(2);
                char endRank = notation.charAt(3);
                int endPosition = PGNExtract.convertFileAndRankToIndex(endFile, endRank);

                moveType = 4;   // standard capture

                if(board.getEnpassant() == endPosition){
                    moveType = 5;   // enpassant capture
                }
                else if(isPromotion){   // promotion capture
                    // the last character in string represents promotion type
                    char promotionType = notation.charAt(notation.indexOf('=') + 1);
                    if(promotionType == 'Q'){
                        // queen promotion
                        moveType = 15;
                    }
                    else if(promotionType == 'N'){
                        moveType = 12;
                    }
                    else if(promotionType == 'B'){
                        moveType = 13;
                    }
                    else if(promotionType == 'R'){
                        moveType = 14;
                    }
                }

                for(int i = 0; i < pieces.getCount(); i++){
                    // find the pawn in the same file with a legal move that ends at end position
                    Piece piece = board.getTile(pieces.occupiedTiles[i]).getPiece();
                    if(piece.isPawn() && Piece.getFile(piece.getPosition()) == startFile){
                        for(Short legalMove : piece.getLegalMoves()){
                            if(MoveGenerator.getEnd(legalMove) == endPosition && MoveGenerator.isCapture(legalMove)){
                                // if the move lands on the end position and is a capture
                                return MoveGenerator.generateMove(piece.getPosition(), endPosition, moveType);
                            }
                        }
                    }
                }
            }
        }
        else{
            // bishop, knight, queen, rook can cause ambiguity (king will have no ambiguity as there is only 1 king at all times)
            /*
             * Possible notations:
             *      - (1) Nc6 <
             *      - (2) B×e5 <
             *      - (3) R1a3 <
             *      - (4) Rdf8 <
             *      - (5) R1xa3 <
             *      - (6) Rdxf8 <
             *      - (7) Qh4e1 <
             *      - (8) Qh4×e1 <
             *
             *      * Consider all 8 cases when generating moves
             */

            if(isCapture){  // capture cases (2, 5, 6, 8)
                moveType = 4;
                if(notation.charAt(1) == 'x'){   // Case 2
                    // no ambiguity capture
                    char endFile = notation.charAt(2);
                    char endRank = notation.charAt(3);
                    int endPosition = PGNExtract.convertFileAndRankToIndex(endFile, endRank);

                    for(int i = 0; i < pieces.getCount(); i++){
                        // find the piece with a legal move that ends at end position
                        Piece piece = board.getTile(pieces.occupiedTiles[i]).getPiece();
                        if(piece.getType() == movingPieceType){
                            for(Short legalMove : piece.getLegalMoves()){
                                if(MoveGenerator.getEnd(legalMove) == endPosition && MoveGenerator.isCapture(legalMove)){
                                    // if the move lands on the end position and is a capture
                                    return MoveGenerator.generateMove(piece.getPosition(), endPosition, moveType);
                                }
                            }
                        }
                    }
                }
                else if(notation.charAt(2) == 'x'){
                    if(Character.isDigit(notation.charAt(1))){  // Case 5
                        char startRank = notation.charAt(1);
                        char endFile = notation.charAt(3);
                        char endRank = notation.charAt(4);
                        int endPosition = PGNExtract.convertFileAndRankToIndex(endFile, endRank);

                        for(int i = 0; i < pieces.getCount(); i++){
                            // find the piece in the same rank with a legal move that ends at end position
                            Piece piece = board.getTile(pieces.occupiedTiles[i]).getPiece();
                            if(piece.getType() == movingPieceType && Piece.getRank(piece.getPosition()) == startRank){
                                for(Short legalMove : piece.getLegalMoves()){
                                    if(MoveGenerator.getEnd(legalMove) == endPosition && MoveGenerator.isCapture(legalMove)){
                                        // if the move lands on the end position and is a capture
                                        return MoveGenerator.generateMove(piece.getPosition(), endPosition, moveType);
                                    }
                                }
                            }
                        }
                    }
                    else{   // Case 6
                        char startFile = notation.charAt(1);
                        char endFile = notation.charAt(3);
                        char endRank = notation.charAt(4);
                        int endPosition = PGNExtract.convertFileAndRankToIndex(endFile, endRank);

                        for(int i = 0; i < pieces.getCount(); i++){
                            // find the piece in the same file with a legal move that ends at end position
                            Piece piece = board.getTile(pieces.occupiedTiles[i]).getPiece();
                            if(piece.getType() == movingPieceType && Piece.getFile(piece.getPosition()) == startFile){
                                for(Short legalMove : piece.getLegalMoves()){
                                    if(MoveGenerator.getEnd(legalMove) == endPosition && MoveGenerator.isCapture(legalMove)){
                                        // if the move lands on the end position and is a capture
                                        return MoveGenerator.generateMove(piece.getPosition(), endPosition, moveType);
                                    }
                                }
                            }
                        }
                    }
                }
                else if(notation.charAt(3) == 'x'){    // Case 8
                    char startFile = notation.charAt(1);
                    char startRank = notation.charAt(2);
                    char endFile = notation.charAt(4);
                    char endRank = notation.charAt(5);
                    int endPosition = PGNExtract.convertFileAndRankToIndex(endFile, endRank);

                    for(int i = 0; i < pieces.getCount(); i++){
                        // find the piece in the same file and rank with a legal move that ends at end position
                        Piece piece = board.getTile(pieces.occupiedTiles[i]).getPiece();
                        if(piece.getType() == movingPieceType && Piece.getFile(piece.getPosition()) == startFile
                                && Piece.getRank(piece.getPosition()) == startRank){
                            for(Short legalMove : piece.getLegalMoves()){
                                if(MoveGenerator.getEnd(legalMove) == endPosition && MoveGenerator.isCapture(legalMove)){
                                    // if the move lands on the end position and is a capture
                                    return MoveGenerator.generateMove(piece.getPosition(), endPosition, moveType);
                                }
                            }
                        }
                    }
                }
            }
            else{   // non - capture cases (1, 3, 4, 7)
                moveType = 0; // quiet move
                int length = notationLength;
                if(!Character.isDigit(notation.charAt(notationLength - 1))){
                    // if the last character in the notation is not a digit
                    // it is a '+' which represents check or '#' which represents checkmate
                    length--;   // ignore the last character
                }
                if(length == 3){    // Case 1
                    char endFile = notation.charAt(1);
                    char endRank = notation.charAt(2);
                    int endPosition = PGNExtract.convertFileAndRankToIndex(endFile, endRank);

                    for(int i = 0; i < pieces.getCount(); i++){
                        // find the piece with a legal move that ends at end position
                        Piece piece = board.getTile(pieces.occupiedTiles[i]).getPiece();
                        if(piece.getType() == movingPieceType){
                            for(Short legalMove : piece.getLegalMoves()){
                                if(MoveGenerator.getEnd(legalMove) == endPosition && !MoveGenerator.isCapture(legalMove)){
                                    // if the move lands on the end position and is a capture
                                    return MoveGenerator.generateMove(piece.getPosition(), endPosition, moveType);
                                }
                            }
                        }
                    }
                }
                else if(length == 4){
                    if(Character.isDigit(notation.charAt(1))){  // Case 3
                        char startRank = notation.charAt(1);
                        char endFile = notation.charAt(2);
                        char endRank = notation.charAt(3);
                        int endPosition = PGNExtract.convertFileAndRankToIndex(endFile, endRank);

                        for(int i = 0; i < pieces.getCount(); i++){
                            // find the piece in the same rank with a legal move that ends at end position
                            Piece piece = board.getTile(pieces.occupiedTiles[i]).getPiece();
                            if(piece.getType() == movingPieceType && Piece.getRank(piece.getPosition()) == startRank){
                                for(Short legalMove : piece.getLegalMoves()){
                                    if(MoveGenerator.getEnd(legalMove) == endPosition && !MoveGenerator.isCapture(legalMove)){
                                        // if the move lands on the end position and is a capture
                                        return MoveGenerator.generateMove(piece.getPosition(), endPosition, moveType);
                                    }
                                }
                            }
                        }
                    }
                    else{   // Case 4
                        char startFile = notation.charAt(1);
                        char endFile = notation.charAt(2);
                        char endRank = notation.charAt(3);
                        int endPosition = PGNExtract.convertFileAndRankToIndex(endFile, endRank);

                        for(int i = 0; i < pieces.getCount(); i++){
                            // find the piece in the same file with a legal move that ends at end position
                            Piece piece = board.getTile(pieces.occupiedTiles[i]).getPiece();
                            if(piece.getType() == movingPieceType && Piece.getFile(piece.getPosition()) == startFile){
                                for(Short legalMove : piece.getLegalMoves()){
                                    if(MoveGenerator.getEnd(legalMove) == endPosition && !MoveGenerator.isCapture(legalMove)){
                                        // if the move lands on the end position and is a capture
                                        return MoveGenerator.generateMove(piece.getPosition(), endPosition, moveType);
                                    }
                                }
                            }
                        }
                    }
                }
                else if(length == 5){   // Case 7
                    char startFile = notation.charAt(1);
                    char startRank = notation.charAt(2);
                    char endFile = notation.charAt(3);
                    char endRank = notation.charAt(4);
                    int endPosition = PGNExtract.convertFileAndRankToIndex(endFile, endRank);

                    for(int i = 0; i < pieces.getCount(); i++){
                        // find the piece in the same file and rank with a legal move that ends at end position
                        Piece piece = board.getTile(pieces.occupiedTiles[i]).getPiece();
                        if(piece.getType() == movingPieceType && Piece.getFile(piece.getPosition()) == startFile
                                && Piece.getRank(piece.getPosition()) == startRank){
                            for(Short legalMove : piece.getLegalMoves()){
                                if(MoveGenerator.getEnd(legalMove) == endPosition && !MoveGenerator.isCapture(legalMove)){
                                    // if the move lands on the end position and is a capture
                                    return MoveGenerator.generateMove(piece.getPosition(), endPosition, moveType);
                                }
                            }
                        }
                    }
                }
            }
        }
        return encodedMove;
    }

    public static int convertFileAndRankToIndex(char file, char rank){
        int row = 8 - Character.getNumericValue(rank);
        int col = file - 'a';

        return (8 * row) + col;
    }

    /**
     * Use the main function to process the PGN files.
     * Once files are processed, shift them into desired directory
     */
    public static void main(String[] args) throws IOException {
        //PGNExtract.processPGN("black2600.pgn");
        //PGNExtract.processPGN("white2600.pgn");

        Board board = new Board();
        board.init(FENUtilities.startFEN);
        short test = PGNExtract.convertNotationToMove(board, board.isWhiteTurn(), "d4");
        // Test convertNotationToMove method
//        URL path = PGNExtract.class.getResource("whiteProcessedBook.txt");
//        File processedPGN = new File(path.getFile());
//        BufferedReader reader = new BufferedReader(new FileReader(processedPGN));
//        String fileLine;
//
//        int index = 0;
//        // build the trie by processing the file line by line for each move
//        while((fileLine = reader.readLine()) != null){
//            String[] moves = fileLine.split(" ");
//            Board board = new Board();
//            board.init(FENUtilities.startFEN);
//            for (String s : moves) {
//                System.out.println(s);
//                if (s.equals("0-1") || s.equals("1-0")) continue;
//                short move = PGNExtract.convertNotationToMove(board, board.isWhiteTurn(), s);
//                //System.out.println(FENUtilities.convertIndexToRankAndFile(MoveGenerator.getStart(move)) + " " + FENUtilities.convertIndexToRankAndFile(MoveGenerator.getEnd(move)) + " " + MoveGenerator.getMoveType(move));
//                Move movement = new Move(board, move);
//                movement.makeMove();
//            }
//            index++;
//            System.out.println(index + "\n");
//        }
    }
}
