import java.io.IOException;
import java.util.Scanner;

public class UCI {
    public static final String ENGINE_NAME = "BLANK";
    public static final int INFINITE_SEARCH = Integer.MAX_VALUE;

    public static String FEN = FENUtilities.startFEN;
    public static Board board;
    public static AI engine;

    /**
     * Implements the UCI protocol (Not all functions are available)
     */
    public static void UCICommunicate() throws IOException {
        // initiates board to starting FEN and initiates engine to turn of FEN
        board = new Board();
        board.init(FEN);
        engine = new AI(board.isWhiteTurn(), board);

        while(true){
            // obtains UCI command from CLI
            Scanner scanner = new Scanner(System.in);
            String command = scanner.nextLine();

            // tells the engine to use UCI protocol. Engine will identify itself and respond with uciok
            if(command.equals("uci")){
                System.out.println("id name " + ENGINE_NAME);
                System.out.println("id author Ryan Lau Q. F.");
                System.out.println("uciok");
            }

            // used by GUI to check if engine is responding
            else if (command.equals("isready")) {
                System.out.println("readyok");
            }

            // prints out the starting FEN of the board
            else if(command.equals("fen")){
                System.out.println(FEN);
            }

            // re-initialise the board and engine
            else if(command.equals("ucinewgame")) {
                board = new Board();
                board.init(FENUtilities.startFEN);
                engine = new AI(board.isWhiteTurn(), board);
            }

            // sets up a fen position on the board
            else if(command.startsWith("position")) {
                parsePosition(command);
            }

            // starts search
            else if(command.equals("go")) {
                boolean useOpeningBook = FEN.equals(FENUtilities.startFEN); // if is startFEN, use opening book
                engine.iterativeDS(INFINITE_SEARCH, useOpeningBook);
            }

            // do perft
            else if(command.startsWith("go perft")){
                String[] input = command.split(" ");
                if(input.length == 3){
                    String depth = input[2];
                    // check if depth is a number
                    boolean isDigit = true;
                    for(int i = 0; i < depth.length(); i++){
                        isDigit = Character.isDigit(depth.charAt(i));
                        if(!isDigit){
                            break;
                        }
                    }
                    if(isDigit){
                        Perft goPerft = new Perft(board);
                        goPerft.perft(Integer.parseInt(depth));
                    }
                }
            }

            // prints out the board
            else if (command.equals("print")) {
                board.state();
            }

//            else if (command.startsWith("setoption")) {
//                // Not implemented yet
//            }

            else if(command.equals("quit")){
                break;
            }
            else{
                System.out.println("Unknown command");
            }
        }
    }

    private static void parsePosition(String input) throws IOException {
        if(input.startsWith("position startpos")){
            board = new Board();
            board.init(FENUtilities.startFEN);
            engine = new AI(board.isWhiteTurn(), board);
        }
        else if(input.startsWith("position fen ")){
            // i.e. position fen
            String fen = input.substring(13);
            String moveString = "";
            if(input.contains("moves")){    // cut the string before the moves are processed
                moveString = fen.substring(fen.indexOf("moves"));
                fen = fen.substring(0, fen.indexOf("moves"));
            }

            FEN = fen.replaceAll("\"", "");;
            board = new Board();
            board.init(FEN);

            // position fen "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1" moves e2a6 b4c3
            if(input.contains("moves")){
                // moves[0] will be "moves";
                // index 1 onwards will be the individual moves made.
                String[] moves = moveString.split(" ");
                for (int i = 1; i < moves.length; i++) {
                    // get start and end index and determine the move type of the move
                    int startPosition = FENUtilities.convertRankAndFileToPosition(moves[i].substring(0, 2));
                    int endPosition = FENUtilities.convertRankAndFileToPosition(moves[i].substring(2, 4));

                    short currentMove = MoveGenerator.generateMove(startPosition, endPosition, determineMoveType(moves[i], board));

                    // make the moves on the board
                    Move movement = new Move(board, currentMove);
                    movement.makeMove();
                }
            }
            engine = new AI(board.isWhiteTurn(), board);
        }

    }


    /**
     * Takes the move information and processes it to get the move type needed to make the move on board
     * @param move refers to the move represented in algebraic notation i.e. e2a6, e7e8q.
     * @param board refers to the board
     * @return move type of the move as documented in MoveGenerator.java
     */
    private static int determineMoveType(String move, Board board) {
        int start = FENUtilities.convertRankAndFileToPosition(move.substring(0, 2));
        int end = FENUtilities.convertRankAndFileToPosition(move.substring(2, 4));

        char promotionType = 0;
        if (move.length() == 5) {
            promotionType = move.charAt(4);
        }

        int moveType = 0;
        if (board.getTile(start).isOccupied()) {
            Piece startPiece = board.getTile(start).getPiece();
            boolean isCapture = board.getTile(end).isOccupied();

            if (isCapture) {
                moveType = 4;
            }

            if (startPiece.isPawn()) {
                // double pawn push
                if (Math.abs(start - end) == 16) {
                    return 1;
                }
                // enpassant capture
                else if (board.getEnpassant() == end) {
                    return 5;
                }
                // promotion
                else if (Piece.getRow(end) == 0 || Piece.getRow(end) == 7) {
                    if (isCapture) {
                        // capture promotion
                        if (promotionType == 'q') {
                            moveType = 15;
                        } else if (promotionType == 'r') {
                            moveType = 14;
                        } else if (promotionType == 'b') {
                            moveType = 13;
                        } else if (promotionType == 'k') {
                            moveType = 12;
                        }
                    } else {
                        // normal promotion
                        if (promotionType == 'q') {
                            moveType = 11;
                        } else if (promotionType == 'r') {
                            moveType = 10;
                        } else if (promotionType == 'b') {
                            moveType = 9;
                        } else if (promotionType == 'k') {
                            moveType = 8;
                        }
                    }
                    return moveType;
                }
            }
            else if (startPiece.isKing()) {
                // check for castling move
                int diff = end - start;
                if (diff == 2) {
                    // king side castling, king position index increases by 2
                    return 2;
                } else if (diff == -2) {
                    // queen side castling, king position index decreases by 2
                    return 3;
                }
            }
        }
        return moveType;
    }

    public static void main(String[] args) throws IOException {
        UCI.UCICommunicate();
    }
}
