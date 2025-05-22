package cn.com.wysha.board.grid_board;


import cn.com.wysha.board.exception.WrongOperationInBoardException;

/**
 * <p>网格状棋盘类
 *
 * <li>对棋盘进行操作
 *
 * <li>判断胜负
 *
 * <p>仅支持双人(人机)对局,将双方分成正方和反方,其中正方的所有棋子用正整数表示,而反方的所有棋子用负整数表示
 *
 * @version 1.0
 * @see BoardGameState
 */
public abstract class AbstractGridBoard {
    /**
     * 空
     */
    public static final byte EMPTY = 0;

    public static final int MAX_POINT = Integer.MAX_VALUE;
    public static final int MIN_POINT = Integer.MIN_VALUE;

    /**
     * 存储棋盘
     */
    private final byte[][] board;
    private final BoardEqualsMode boardEqualsMode;
    protected long state = 0;

    /**
     * 创建空棋盘
     *
     * @param width           宽度
     * @param height          高度
     * @param boardEqualsMode 棋盘匹配方式
     */
    protected AbstractGridBoard(int width, int minWidth, int height, int minHeight, BoardEqualsMode boardEqualsMode) {
        this.boardEqualsMode = boardEqualsMode;
        if ((width < minWidth) || (height < minHeight))
            throw new IllegalArgumentException("Wrong size!");
        if ((width < 3) || (height < 3))
            throw new IllegalArgumentException("Wrong size!");
        board = new byte[width][height];
    }

    public BoardEqualsMode getBoardEqualsMode() {
        return boardEqualsMode;
    }

    /**
     * 工厂方法
     */
    public abstract AbstractGridBoard copy();

    public final void copyData(AbstractGridBoard s) {
        int w = getWidth();
        int h = getHeight();
        for (int i = 0; i < w; i++) {
            System.arraycopy(s.board[i], 0, this.board[i], 0, h);
        }
    }

    public final long getState() {
        return state;
    }

    /**
     * 在多人对局中检查棋盘是否同步
     *
     * @param state 状态值
     * @return 状态值是否一样
     */
    public final boolean checkState(long state) {
        return getState() == state;
    }

    /**
     * 获取宽度
     *
     * @return 宽度
     */
    public final int getWidth() {
        return board.length;
    }

    /**
     * 获取高度
     *
     * @return 高度
     */
    public final int getHeight() {
        return board[0].length;
    }

    /**
     * <p>分析当前局面
     *
     * @param side 此时轮到某一方
     * @return 分数, 大于等于 {@code MIN_POINT} ,小于等于 {@code MAX_POINT} ,数值越小越利于反方,反之,数值越大越利于正方
     */
    public abstract int getPoint(Side side);

    /**
     * 设置坐标处棋子类型
     *
     * @param x     横坐标
     * @param y     纵坐标
     * @param chess 棋子类型
     */
    public void setChessInBoard(int x, int y, byte chess) throws WrongOperationInBoardException {
        if ((x >= getWidth()) || (y >= getHeight()) || (x < 0) || (y < 0))
            throw new WrongOperationInBoardException();
        board[x][y] = chess;
        state++;
    }

    /**
     * 根据坐标获取棋子类型,如果坐标处为空,则返回{@code EMPTY}
     *
     * @param x 横坐标
     * @param y 纵坐标
     * @return 棋子类型
     */
    public final byte getChessInBoard(int x, int y) throws WrongOperationInBoardException {
        if ((x >= getWidth()) || (y >= getHeight()) || (x < 0) || (y < 0))
            throw new WrongOperationInBoardException();
        return board[x][y];
    }

    public abstract int compare(int x1,int y1, int x2, int y2);


    /**
     * 获取游戏状态
     *
     * @return 游戏状态
     */
    public abstract BoardGameState getBoardGameState();
}
