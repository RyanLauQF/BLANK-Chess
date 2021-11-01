public class Player extends AI{
    protected final boolean isHuman;

    Player(boolean isWhite, boolean isHuman, Board board){
        super(isWhite, board, true);
        this.isHuman = isHuman;
    }

    public Board getBoard(){
        return board;
    }

    public boolean isWhite(){
        return isWhite;
    }

    public boolean isHuman(){
        return isHuman;
    }
}
