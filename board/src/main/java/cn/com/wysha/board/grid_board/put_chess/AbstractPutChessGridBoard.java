package cn.com.wysha.board.grid_board.put_chess;

import cn.com.wysha.board.exception.WrongOperationInBoardException;
import cn.com.wysha.board.grid_board.AbstractGridBoard;
import cn.com.wysha.board.grid_board.BoardEqualsMode;
import cn.com.wysha.board.grid_board.BoardGameState;
import cn.com.wysha.board.grid_board.BoardPositionState;

/**
 * <p>每次操作为放置棋子的棋盘类
 *
 * @version 1.0
 */
public abstract class AbstractPutChessGridBoard extends AbstractGridBoard {

    public static final byte P_CHESS = 1;
    public static final byte N_CHESS = -1;

    /**
     * 创建空棋盘
     *
     * @param width           宽度
     * @param minWidth        最小宽度
     * @param height          高度
     * @param minHeight       最小高度
     * @param boardEqualsMode 棋盘匹配方式
     */
    protected AbstractPutChessGridBoard(int width, int minWidth, int height, int minHeight, BoardEqualsMode boardEqualsMode) {
        super(width, minWidth, height, minHeight, boardEqualsMode);
    }

    /**
     * 是否不允许在坐标处落子
     *
     * @param x     横坐标
     * @param y     纵坐标
     * @param chess 棋子类型
     * @return 是否不允许
     */
    public boolean isNotAllowPutChess(int x, int y, byte chess) throws WrongOperationInBoardException {
        return getChessInBoard(x, y) != EMPTY;
    }

    /**
     * 在放置棋子后获取游戏状态,传入的坐标为要放置的棋子,用于自动结束游戏
     *
     * @param x 横坐标
     * @param y 纵坐标
     * @return 游戏状态
     */
    protected abstract BoardGameState getBoardGameStateLast(int x, int y, byte chess) throws WrongOperationInBoardException;


    /**
     * 悔棋
     *
     * @param boardPositionState 原棋盘状态,用于一步棋能够改变多个坐标处棋子的游戏类型
     * @param x                  横坐标
     * @param y                  纵坐标
     */
    public void undo(BoardPositionState boardPositionState, int x, int y) throws WrongOperationInBoardException {
        setChessInBoard(x, y, EMPTY);
    }

    /**
     * 在坐标处落子
     *
     * @param x     横坐标
     * @param y     纵坐标
     * @param chess 棋子类型
     * @return 游戏状态
     */
    public final BoardGameState putChess(int x, int y, byte chess) throws WrongOperationInBoardException {
        if (chess == EMPTY)
            throw new WrongOperationInBoardException();
        if ((x >= getWidth()) || (y >= getHeight()) || (x < 0) || (y < 0))
            throw new WrongOperationInBoardException();
        if (isNotAllowPutChess(x, y, chess))
            throw new WrongOperationInBoardException();
        setChessInBoard(x, y, chess);
        return getBoardGameStateLast(x, y, chess);
    }
}