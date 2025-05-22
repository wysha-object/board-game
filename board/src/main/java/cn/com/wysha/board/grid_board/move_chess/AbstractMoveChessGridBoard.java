package cn.com.wysha.board.grid_board.move_chess;

import cn.com.wysha.board.exception.WrongOperationInBoardException;
import cn.com.wysha.board.grid_board.AbstractGridBoard;
import cn.com.wysha.board.grid_board.BoardEqualsMode;
import cn.com.wysha.board.grid_board.BoardGameState;
import cn.com.wysha.board.grid_board.BoardPositionState;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>每次操作为移动棋子的棋盘类
 *
 * @version 1.0
 */
public abstract class AbstractMoveChessGridBoard extends AbstractGridBoard {
    /**
     * key = x + ( y << 32 )
     * value[i] = { x, y }
     */
    private final Map<Long, int[][]> positionMap = new HashMap<>();

    /**
     * 创建空棋盘
     *
     * @param width           宽度
     * @param height          高度
     * @param boardEqualsMode 棋盘匹配方式
     */
    protected AbstractMoveChessGridBoard(int width, int minWidth, int height, int minHeight, BoardEqualsMode boardEqualsMode) {
        super(width, minWidth, height, minHeight, boardEqualsMode);
    }

    protected final void clearTemp() {
        positionMap.clear();
    }

    /**
     * <p>获取坐标处棋子所有可以移动到的坐标
     *
     * <p>检查是否已经获取过,如果已经获取过,则返回以前获得的数据
     *
     * @return 可以移动到的坐标 int [ index ] = { d_x, d_y }
     */
    public final int[][] getCanMovePosition(int s_x, int s_y) {
        long t_p = s_x + ((long) s_y << 32);
        int[][] tmp = positionMap.get(t_p);
        if (tmp == null) {
            tmp = calCanMovePosition(s_x, s_y);
            positionMap.put(t_p, tmp);
        }
        return tmp;
    }

    /**
     * 获取坐标处棋子所有可以移动到的坐标
     *
     * @return 可以移动到的坐标 int [ index ] = { d_x, d_y }
     */
    protected abstract int[][] calCanMovePosition(int s_x, int s_y);

    /**
     * 移动棋子并获取游戏状态
     *
     * @return 游戏状态
     */
    protected abstract BoardGameState doMoveChess(int s_x, int s_y, int d_x, int d_y);

    /**
     * 悔棋
     *
     * @param boardPositionState 原棋盘状态
     * @param sX                 横坐标
     * @param sY                 纵坐标
     * @param dX                 横坐标
     * @param dY                 纵坐标
     */
    public void undo(BoardPositionState boardPositionState, int sX, int sY, int dX, int dY) throws WrongOperationInBoardException {
        setChessInBoard(dX, dY, getChessInBoard(sX, sY));
        clearTemp();
    }

    /**
     * 移动棋子
     *
     * @param s_x 源横坐标
     * @param s_y 源纵坐标
     * @param d_x 目标横坐标
     * @param d_y 目标纵坐标
     * @return 游戏状态
     */
    public final BoardGameState moveChess(int s_x, int s_y, int d_x, int d_y) throws WrongOperationInBoardException {
        if (getChessInBoard(s_x, s_y) == EMPTY)
            throw new WrongOperationInBoardException();
        if ((s_x >= getWidth()) || (s_y >= getHeight()) || (s_x < 0) || (s_y < 0) || (d_x >= getWidth()) || (d_y >= getHeight()) || (d_x < 0) || (d_y < 0))
            throw new WrongOperationInBoardException();

        int[][] tmp = positionMap.get(s_x + ((long) s_y << 32));
        boolean b = true;
        for (int[] p : tmp) {
            if ((p[0] == d_x) && (p[1] == d_y)) {
                b = false;
                break;
            }
        }
        if (b)
            throw new WrongOperationInBoardException();

        clearTemp();
        return doMoveChess(s_x, s_y, d_x, d_y);
    }
}