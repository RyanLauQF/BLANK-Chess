public class GameStatus {
    private static boolean whiteTurn = false;
    private static boolean isStaleMate = false;
    private static boolean isCheckMate = false;
    private static boolean isDraw = false;

    public static boolean checkGameEnded(Board board){
        if(board.getAllLegalMoves().size() == 0){
            // no legal moves left, means check mated
            boolean isWhiteTurn = board.isWhiteTurn();
            whiteTurn = board.isWhiteTurn();
            if(checkStaleMate(board)){
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
        if(board.getWhitePieces().getCount() == 1 && board.getBlackPieces().getCount() == 1 || board.repetitionHistory.get(board.getZobristHash()) == 3){
            isDraw = true;
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
        else if(isDraw){
            state = "Game has ended in a Draw!";
        }
        return state;
    }

    private static boolean checkStaleMate(Board board){
        // set move to opponents turn to check if they have any moves left
        int kingPosition;
        if(board.isWhiteTurn()){
            kingPosition = board.getWhiteKingPosition();
        }
        else{
            kingPosition = board.getBlackKingPosition();
        }
        return !board.isTileAttacked(kingPosition, board.isWhiteTurn());
    }
}

