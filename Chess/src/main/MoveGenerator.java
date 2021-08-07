public class MoveGenerator {
    /*
     *  Creates a move by splitting a short of 16 bits into 3 sections (one 4-bit and two 6-bit sections)
     *  the 4 bit section will represent the move types while the 6 bit sections will represent
     *  the start and end position of the move (0 - 63)
     *
     *  How the 16 bits are split:
     *
     *           0000 000000 000000
     *      moveType | start | end
     *
     *  The moves types are represented using the 4 bits
     *  the first bit from the left checks for promotion
     *  the second bit from the left checks if the move is a capture
     *
     *          0    |    0    | 0 | 0
     *     promotion | capture | - | -
     *
     *  Move types:
     *  0000 (0) - quiet move (no captures, just a move to empty square)
     *  0001 (1) - double pawn push
     *  0010 (2) - king side castle
     *  0011 (3) - queen side castle
     *  0100 (4) - capture
     *  0101 (5) - enpassant capture
     *  1000 (8) - knight promotion
     *  1001 (9) - bishop promotion
     *  1010 (10) - rook promotion
     *  1011 (11) - queen promotion
     *  1100 (12) - knight capture promotion
     *  1101 (13) - bishop capture promotion
     *  1110 (14) - rook capture promotion
     *  1111 (15) - queen capture promotion
     *
     *  E.g. a "double pawn push" from starting tile 48 to ending tile 32
     *        moveType == 0001
     *        start == 110000
     *        end == 100000
     *
     *     -> move == 0001 110000 100000
     */

    // bit masks to decode 16 bit move
    private static final int START_MASK = 0b0000111111000000;
    private static final int END_MASK = 0b0000000000111111;
    private static final int MOVE_TYPE_MASK = 0b1111000000000000;
    private static final int PROMOTION_MASK = 0b1000;
    private static final int CAPTURE_MASK = 0b0100;

    /**
     * Creates the move based off above-mentioned documentation
     * @param start refers to the starting position
     * @param end refers to the end position to the move
     * @param moveType refers to the type of move being made
     * @return the generated move stored in a 16 bit short data type
     */
    public static short generateMove(int start, int end, int moveType){
        return (short) ((moveType & 0xf) << 12 | end & 0x3f | ((start & 0x3f) << 6));
    }

    /**
     * Gets the start tile of a move by use a bitwise AND (&) with the START_MASK
     * and then bit shifts the value towards zero by 6 bits
     * @param move refers to the 16 bit encoded move
     * @return start position of the move
     */
    public static int getStart(short move){
        return (move & START_MASK) >>> 6;
    }

    /**
     * Gets the end position of a move by use a bitwise AND (&) with the END_MASK
     * Do not need to bit shift as end position is already flushed to the right of the binary number
     * @param move refers to the 16 bit encoded move
     * @return end position of the move
     */
    public static int getEnd(short move){
        return move & END_MASK;
    }

    /**
     * Gets the end position of a move by use a bitwise AND (&) with the END_MASK
     * and then bit shifts the value towards zero by 12 bits
     * @param move refers to the 16 bit encoded move
     * @return end position of the move
     */
    public static int getMoveType(short move){
        return (move & MOVE_TYPE_MASK) >>> 12;
    }

    /**
     * Checks if the move is a promotion by using bitwise AND with PROMOTION_MASK
     * @param move refers to the 16 bit encoded move
     * @return true if the 4th bit is set (equals to 8)
     */
    public static boolean isPromotion(short move){
        int moveType = getMoveType(move);
        return (moveType & PROMOTION_MASK) == 8;
    }

    /**
     * Checks if the move is a capture by using bitwise AND with CAPTURE_MASK
     * @param move refers to the 16 bit encoded move
     * @return true if the 3rd bit is set (equals to 4)
     */
    public static boolean isCapture(short move){
        int moveType = getMoveType(move);
        return (moveType & CAPTURE_MASK) == 4;
    }

    /**
     * Checks if the move is a castling move
     * @param move refers to the 16 bit encoded mpve
     * @return true if the move type is equal to 2 / 3 for king or queen side castling respectively
     */
    public static boolean isCastling(short move){
        int moveType = getMoveType(move);
        return moveType == 2 || moveType == 3;
    }

    /**
     * Used for to convert the moves to rank and file
     */
    public static String toString(short move){
        int start = MoveGenerator.getStart(move);
        int end = MoveGenerator.getEnd(move);

        return FENUtilities.convertIndexToRankAndFile(start) + FENUtilities.convertIndexToRankAndFile(end);
    }

//    /**
//     * unit testing
//     */
//    public static void main(String[] args){
//        // make the move and get output
//        int start = 48;
//        int end = 32;
//        int moveType = 12;
//        short move = generateMove(start, end, moveType);
//
//        System.out.println("Binary: " + Integer.toBinaryString(move));
//        System.out.println("Start position: " + getStart(move));
//        System.out.println("End position: " + getEnd(move));
//        System.out.println("Move Type: " + getMoveType(move));
//        System.out.println(isCapture(move));
//    }
}

