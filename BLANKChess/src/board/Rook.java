import java.util.ArrayList;

public class Rook extends Piece{

    /*
     * Move Rules:
     * 1) Rook only moves/attacks vertically or horizontally (up, down, left, right)
     * 2) Rook can jump to castle with king
     *
     * Rook moves by sliding. Check if piece is blocked by any piece. If piece is enemy, attack is possible.
     * Similar implementation to a Bishop
     */

    public static final int ROOK_MG_VALUE = 477, ROOK_EG_VALUE = 512;

    public Rook(boolean isWhite, int position, Board b){
        super(isWhite, position, b);
        this.type = PieceType.ROOK;
    }

    @Override
    public ArrayList<Short> getPossibleMoves(boolean generateCapturesOnly){
        ArrayList<Short> list = new ArrayList<>();
        int end, offSet;
        Tile endTile;
        int[] directions = MoveDirections.getDirections(getPosition());
        for(int index = 0; index < 4; index++){ // straight offsets start at index 0 to 3
            offSet = MoveDirections.directionOffSets[index];
            for(int i = 0; i < directions[index]; i++){
                end = getPosition() + (offSet * (i + 1));
                endTile = super.board.getTile(end);
                if(endTile.isOccupied()){
                    if(endTile.getPiece().isWhite() != this.isWhite()){
                        // enemy piece capture
                        list.add(MoveGenerator.generateMove(getPosition(), end, 4));
                    }
                    break;
                }
                else{
                    if(!generateCapturesOnly){
                        // standard movement with no capture
                        list.add(MoveGenerator.generateMove(getPosition(), end, 0));
                    }
                }
            }
        }
        return list;
    }

    private boolean isSeventhRankRook(){
        if(isWhite()){
            return Piece.getRow(getPosition()) == 1;
        }
        else{
            return Piece.getRow(getPosition()) == 6;
        }
    }

    /**
     * Analyses the file the rook is currently on to check if it is an open, semi-open or closed file
     * @return 1, 2, 3 for open, closed or semi-open file respectively
     */
    private int rookFileType(){
        int squaresToTop = getRow(getPosition());
        int squaresToBot = 7 - getRow(getPosition());

        boolean enemyPawnFound = false;
        boolean alliedPawnFound = false;

        int endPosition;
        Piece piece;

        // go towards top edge
        for(int i = 0; i < squaresToTop; i++){
            endPosition = getPosition() + ((-8) * (i + 1));
            if(board.getTile(endPosition).isOccupied()){
                piece = board.getTile(endPosition).getPiece();
                if(piece.isPawn()){
                    if(piece.isWhite()){
                        alliedPawnFound = true;
                    }
                    else{
                        enemyPawnFound = true;
                    }
                }
            }
        }

        for(int i = 0; i < squaresToBot; i++){
            endPosition = getPosition() + ((8) * (i + 1));
            if(board.getTile(endPosition).isOccupied()){
                piece = board.getTile(endPosition).getPiece();
                if(piece.isPawn()){
                    if(piece.isWhite()){
                        alliedPawnFound = true;
                    }
                    else{
                        enemyPawnFound = true;
                    }
                }
            }
        }

        // open file as no allied or enemy pawns in file
        if(!alliedPawnFound && !enemyPawnFound){
            return 1;
        }
        // closed file if file contains both allied and enemy pawns
        else if(alliedPawnFound && enemyPawnFound){
            return 2;
        }
        // semi open file contains no allied pawns
        else if(!alliedPawnFound){
            return 3;
        }
        return 0;
    }

    @Override
    public int getExtraEval(){
        int positionBonus = 0;

        if(isSeventhRankRook()){    // rook on rank 7 is able to create a major threat
            positionBonus += 41;
        }

        int rookFileType = rookFileType();
        if(rookFileType == 1){  // open file rook
            positionBonus += 27;
        }
        else if(rookFileType == 2){ // closed file rook
            positionBonus -= 46;
        }
        else if(rookFileType == 3){ // semi-open file rook
            positionBonus += 57;
        }

        return positionBonus;
    }

    @Override
    public int getMidGameValue(){
        int positionBonus = (isWhite()) ? EvalUtilities.rookMidGamePST[getPosition()] : EvalUtilities.rookMidGamePST[EvalUtilities.blackFlippedPosition[getPosition()]];
        return ROOK_MG_VALUE + positionBonus;
    }

    @Override
    public int getEndGameValue(){
        int positionBonus = (isWhite()) ? EvalUtilities.rookEndGamePST[getPosition()] : EvalUtilities.rookEndGamePST[EvalUtilities.blackFlippedPosition[getPosition()]];
        return ROOK_EG_VALUE + positionBonus;
    }

    @Override
    public int getPhaseValue(){
        return ROOK_MG_VALUE;
    }

    @Override
    public String toString(){
        return "R";
    }
}
