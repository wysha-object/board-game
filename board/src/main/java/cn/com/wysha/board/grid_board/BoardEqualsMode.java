package cn.com.wysha.board.grid_board;

public enum BoardEqualsMode {
    /**
     * 完全匹配
     */
    FULL,
    /**
     * 完全匹配\
     * 上下对称
     */
    UP_DOWN,
    /**
     * 完全匹配\
     * 左右对称
     */
    LEFT_RIGHT,
    /**
     * 完全匹配\
     * 中心对称
     */
    ALL
}
