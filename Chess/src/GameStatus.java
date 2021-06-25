
public class GameStatus {
    public static boolean checkGameEnded(Board board){
        if(board.getAllLegalMoves().size() == 0){
            // no legal moves left, means check mated
            boolean isWhiteTurn = board.isWhiteTurn();
            if(checkDraw(board)){
                System.out.println("Game has ended in a Stalemate!");
                return true;
            }
            if(isWhiteTurn){
                System.out.println("Black has Won!");
            }
            else{
                System.out.println("White has Won!");
            }
            return true;
        }
        return false;
    }

    private static boolean checkDraw(Board board){
        // set move to opponents turn to check if they have any moves left
        board.setTurn(!board.isWhiteTurn());
        return board.getAllLegalMoves().size() == 0;
    }
}

