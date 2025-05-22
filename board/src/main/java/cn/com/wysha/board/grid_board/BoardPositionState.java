package cn.com.wysha.board.grid_board;

import cn.com.wysha.board.exception.WrongOperationInBoardException;

import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;

public class BoardPositionState implements Serializable {
    @Serial
    private static final long serialVersionUID = 0L;

    /**
     * 棋盘状态
     */
    private final int[][] board;

    /**
     * 此时轮到的某一方
     */
    private final Side side;

    private final BoardEqualsMode boardEqualsMode;

    private transient Integer hashCode;

    /**
     * 最后一次成功匹配采用的匹配方式
     */
    private transient BoardEqualsMode last_BoardEqualsMode;

    /**
     * @param boardEqualsMode   棋盘的匹配方式
     * @param abstractGridBoard 目标棋盘
     * @param side              此时轮到的某一方
     */
    public BoardPositionState(BoardEqualsMode boardEqualsMode, AbstractGridBoard abstractGridBoard, Side side) {
        this.boardEqualsMode = boardEqualsMode;
        int w = abstractGridBoard.getWidth();
        int h = abstractGridBoard.getHeight();
        board = new int[w][h];
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                try {
                    board[x][y] = abstractGridBoard.getChessInBoard(x, y);
                } catch (WrongOperationInBoardException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        this.side = side;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof BoardPositionState boardPositionState) {
            if (side != boardPositionState.side) return false;

            int w = this.board.length;
            int h = this.board[0].length;

            if (boardPositionState.board.length != w) return false;
            if (boardPositionState.board[0].length != h) return false;

            if (boardEqualsMode == BoardEqualsMode.FULL) {
                return Arrays.deepEquals(board, boardPositionState.board);
            } else {
                boolean[] booleans = new boolean[4];
                switch (boardEqualsMode) {
                    case UP_DOWN -> {
                        booleans[0] = true;
                        booleans[1] = true;
                    }
                    case LEFT_RIGHT -> {
                        booleans[0] = true;
                        booleans[2] = true;
                    }
                    case ALL -> Arrays.fill(booleans, true);
                    default -> throw new IllegalArgumentException();
                }

                for (int x = 0; x < board.length; x++) {
                    int x_ = w - 1 - x;
                    for (int y = 0; y < board[0].length; y++) {
                        int y_ = h - 1 - y;

                        int tmp_v = board[x][y];
                        if (booleans[0]) {
                            booleans[0] = tmp_v == boardPositionState.board[x][y];
                        }
                        if (booleans[1]) {
                            booleans[1] = tmp_v == boardPositionState.board[x][y_];
                        }
                        if (booleans[2]) {
                            booleans[2] = tmp_v == boardPositionState.board[x_][y];
                        }
                        if (booleans[3]) {
                            booleans[3] = tmp_v == boardPositionState.board[x_][y_];
                        }

                        boolean tmp = true;
                        for (boolean b : booleans) {
                            if (b) {
                                tmp = false;
                                break;
                            }
                        }
                        if (tmp) {
                            return false;
                        }
                    }
                }

                if (booleans[0]) {
                    last_BoardEqualsMode = BoardEqualsMode.FULL;
                    return true;
                } else if (booleans[1]) {
                    last_BoardEqualsMode = BoardEqualsMode.UP_DOWN;
                    return true;
                } else if (booleans[2]) {
                    last_BoardEqualsMode = BoardEqualsMode.LEFT_RIGHT;
                    return true;
                } else if (booleans[3]) {
                    last_BoardEqualsMode = BoardEqualsMode.ALL;
                    return true;
                }
            }
        }
        return false;
    }

    public BoardEqualsMode getLast_BoardEqualsMode() {
        return last_BoardEqualsMode;
    }


    @Override
    public int hashCode() {
        if (hashCode != null) {
            return hashCode;
        }
        int result = Arrays.deepHashCode(board);
        hashCode = result;
        return result;
    }
}
