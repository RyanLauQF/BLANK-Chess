import java.io.IOException;

public class Player extends AI{
    protected final boolean isHuman;

    Player(boolean isWhite, boolean isHuman, Board board) throws IOException {
        super(isWhite, board);
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
    /**
     * unit testing
     */
    public static void main(String[] args) {

    }
}
