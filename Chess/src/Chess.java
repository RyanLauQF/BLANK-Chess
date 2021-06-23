import java.util.Scanner;

/*
 * TODO:
 *  1) Complete CheckGameEnd class to check if a move is legal and does not result in a mate
 *  2) Implement castling and En passant
 *  3) Implement Move method to move pieces on a board (based on any possible legal moves)
 *  4) Chess game should now be working.
 *  5) *Build engine based on working game
 *
 */

public class Chess {
    private Board board;

    public Chess(){
        // initiate board
        board = new Board();
        board.init(FENUtilities.startFEN);  // default chess starting FEN notation
        board.state();
    }

    public Chess(String FEN){
        // initiate board
        board = new Board();
        board.init(FEN);
        board.state();
    }

    public boolean hasEnded(){
        // check if the game ended
        // checkmate, draw, stalemate
        return true;
    }

    public static void main(String[] args) {
        // start game using FEN notation
        Scanner sc = new Scanner(System.in);
        System.out.println("Start a default game? (y/n)");

        String s = sc.nextLine();
        if(s.equals("y")){
            Chess game = new Chess();
        }
        else if (s.equals("n")){
            System.out.print("Input custom FEN position: ");
            String FEN = sc.nextLine();
            Chess game = new Chess(FEN);
        }
        else {
            System.out.println("Input either 'y' or 'n'");
        }
    }
}


