import java.util.ArrayList;

public final class MoveDirections {
    // Stores pre-calculated data of number of squares to the edges of the board in every direction for every square on the board
    private static final int[][] directions;
    // Stores pre-calculated data of all possible knight moves at every square of the board
    private static final int[][] knightDirections;

    // offsets are the changes in index at a position to travel in the respective directions
    // index 0 to 3 will represent the straight directions
    // index 0 - to top edge, index 1 - to bot edge, index 2 - to left edge, index 3 - to right edge
    // index 4 - 7 will represent the diagonal directions
    public static final int[] directionOffSets = {-8, 8, -1, 1, -7, -9, 9, 7};

    // Knight offsets for movement. Knight moves in an L shape
    public static final int[] knightOffSets = {-6, 6, -10, 10, -15, 15, -17, 17};

    public static final int[] whitePawnAttackDirection = {-7, -9};
    public static final int[] blackPawnAttackDirection = {7, 9};

    static {
        // chess board has 64 tiles
        // get the number of squares to each edge from each tile
        directions = new int[64][];
        knightDirections = new int[64][];
        generateAllDirections();
        generateKnightDirections();
    }

    private static void generateAllDirections(){
        for(int i = 0; i < 64; i++){
            // straight distances
            int toTopEdge = getRow(i);
            int toBotEdge = 7 - getRow(i);
            int toLeftEdge = getCol(i);
            int toRightEdge = 7 - getCol(i);

            // diagonal directions
            int northEast = Math.min(toTopEdge, toRightEdge);
            int northWest = Math.min(toTopEdge, toLeftEdge);
            int southEast = Math.min(toBotEdge, toRightEdge);
            int southWest = Math.min(toBotEdge, toLeftEdge);

            int[] toEdges = new int[8];
            toEdges[0] = toTopEdge;
            toEdges[1] = toBotEdge;
            toEdges[2] = toLeftEdge;
            toEdges[3] = toRightEdge;
            toEdges[4] = northEast;
            toEdges[5] = northWest;
            toEdges[6] = southEast;
            toEdges[7] = southWest;

            directions[i] = toEdges;
        }
    }

    private static void generateKnightDirections(){
        int endPosition;
        ArrayList<Integer> possibleMoves;
        for(int i = 0; i < 64; i++){
            possibleMoves = new ArrayList<>();
            for(int j = 0; j < 8; j++){
                endPosition = i + knightOffSets[j];
                if(isValidKnightMove(i, endPosition)){
                    possibleMoves.add(endPosition);
                }
            }
            int[] knightMoves = new int[possibleMoves.size()];
            for(int moves = 0; moves < possibleMoves.size(); moves++){
                knightMoves[moves] = possibleMoves.get(moves);
            }
            knightDirections[i] = knightMoves;
        }
    }

    private static boolean isValidKnightMove(int start, int end) {
        if(start < 0 || start > 63 || end < 0 || end > 63) {
            return false;   // out of bounds
        }
        // check if direction of movement is possible based on knights position
        // get the sum of differences between end position and start position to check L-shape movement
        // for an L-shape movement, the sum must be equal to 3.
        // return true if sum == 3
        return Math.abs(getRow(start) - getRow(end)) + Math.abs(getCol(start) - getCol(end)) == 3;
    }

    public static int[] getDirections(int position){
        return directions[position];
    }

    public static int[] getKnightDirections(int position){
        return knightDirections[position];
    }

    private static int getRow(int position){
        return (position - (position % 8)) / 8;
    }

    private static int getCol(int position){
        return position % 8;
    }
}
