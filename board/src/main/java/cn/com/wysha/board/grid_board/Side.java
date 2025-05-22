package cn.com.wysha.board.grid_board;

/**
 * 棋盘上的某一方
 */
public enum Side {
    /**
     * 正方
     */
    POSITIVE,
    /**
     * 反方
     */
    NEGATIVE,
    /**
     * 无
     */
    NONE;

    public static Side getSideByChess(int chess) {
        if (chess == AbstractGridBoard.EMPTY) return NONE;
        if (chess > 0) return POSITIVE;
        return NEGATIVE;
    }

    public static Side nextSide(Side side) {
        if (side == POSITIVE) return NEGATIVE;
        if (side == NEGATIVE) return POSITIVE;
        return NONE;
    }
}