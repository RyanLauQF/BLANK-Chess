public class GameStatus {
    private static boolean whiteTurn = false;
    private static boolean isStaleMate = false;
    private static boolean isCheckMate = false;

    public static boolean checkGameEnded(Board board){
        if(board.getAllLegalMoves().size() == 0){
            // no legal moves left, means check mated
            boolean isWhiteTurn = board.isWhiteTurn();
            whiteTurn = board.isWhiteTurn();
            if(checkDraw(board)){
                System.out.println("Game has ended in a Stalemate!");
                isStaleMate = true;
                return true;
            }
            if(isWhiteTurn){
                System.out.println("Black has Won!");
            }
            else{
                System.out.println("White has Won!");
            }
            isCheckMate = true;
            return true;
        }
        return false;
    }

    public static String getHowGameEnded(){
        String state = null;
        if(isStaleMate){
            state = "Game has ended in a Stalemate!";
        }
        else if(isCheckMate){
            if(whiteTurn){
                state = "Black has Won!";
            }
            else{
                state = "White has Won!";
            }
        }
        return state;
    }

    private static boolean checkDraw(Board board){
        // set move to opponents turn to check if they have any moves left
        board.setTurn(!board.isWhiteTurn());
        return board.getAllLegalMoves().size() == 0;
    }
}

