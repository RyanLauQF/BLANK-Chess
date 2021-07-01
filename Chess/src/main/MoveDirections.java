public class MoveDirections {
    private final int[][] direction;

    public MoveDirections(){
        // chess board has 64 tiles
        // get the number of squares to each edge from each tile
        this.direction = new int[64][];
        for(int i = 0; i < 64; i++){
            // straight distances
            int toTopEdge = getRow(i);
            int toBotEdge = 7 - getRow(i);
            int toLeftEdge = getCol(i);
            int toRightEdge = 7 - getCol(i);

            // TODO diagonal distances

            int[] toEdges = {toTopEdge, toBotEdge, toLeftEdge, toRightEdge};
            direction[i] = toEdges;
        }
    }

    public int[] getDirections(int position){
        return this.direction[position];
    }

    private static int getRow(int position){
        return (position - (position % 8)) / 8;
    }

    private static int getCol(int position){
        return position % 8;
    }
}
