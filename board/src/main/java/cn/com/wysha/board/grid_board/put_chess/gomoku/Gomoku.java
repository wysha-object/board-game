package cn.com.wysha.board.grid_board.put_chess.gomoku;

import cn.com.wysha.board.exception.WrongOperationInBoardException;
import cn.com.wysha.board.grid_board.*;
import cn.com.wysha.board.grid_board.put_chess.AbstractPutChessGridBoard;

/**
 * 五子棋
 * 若使用最大最小值算法的AI,推荐搜索深度(3 <= deepin <= 6)
 * 建议的棋盘大小{15*15,9*9,13*13,18*18,19*19}
 */
public class Gomoku extends AbstractPutChessGridBoard {
    /**
     * 方向数组
     */
    protected static final int[][] DIRECTIONS = {{-1, 1}, {1, 1}, {0, 1}, {1, 0}};
    /**
     * LINE_POINT[i] = i个棋子相连得分
     */
    private static final int[] LINE_POINT = {0, 64, 128, 256, 512};
    /**
     * 一个活四算作两个冲四
     * 冲四的出现次数
     */
    private int timesFourOnLine;
    /**
     * 正方活三的出现次数
     */
    private int p_timesThreeOnLine;
    /**
     * 反方活三的出现次数
     */
    private int n_timesThreeOnLine;

    public Gomoku(int width, int height) {
        super(width, 5, height, 5, BoardEqualsMode.ALL);
    }

    @Override
    public AbstractGridBoard copy() {
        Gomoku gomoku = new Gomoku(this.getWidth(), this.getHeight());
        gomoku.copyData(this);
        return gomoku;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getPoint(Side side) {
        return getBoardGameState(side).point();
    }

    @Override
    public int compare(int x1, int y1, int x2, int y2) {
        int i1 = Math.min(x1, getWidth() - 1 - x1) * Math.min(y1, getHeight() - 1 - y1);
        int i2 = Math.min(x2, getWidth() - 1 - x2) * Math.min(y2, getHeight() - 1 - y2);
        return Integer.compare(i2, i1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BoardGameState getBoardGameState() {
        return getBoardGameState(Side.NONE);
    }

    public BoardGameState getBoardGameState(Side side) {
        timesFourOnLine = 0;
        p_timesThreeOnLine = 0;
        n_timesThreeOnLine = 0;

        int point = 0;

        int w = getWidth();
        int h = getHeight();
        int tmp = Math.max(w, h);
        boolean isDRAW = true;

        for (int[] dir : DIRECTIONS) {
            int startX, startY;
            int endX, endY;

            if (dir[0] == -1 && dir[1] == 1) {
                startX = 0;
                startY = 0;
                endX = -tmp;
                endY = tmp;
            } else if (dir[0] == 1 && dir[1] == 1) {
                startX = w - 1;
                startY = 0;
                endX = startX + tmp;
                endY = startY + tmp;
            } else if (dir[0] == 0 && dir[1] == 1) {
                startX = 0;
                startY = 0;
                endX = 0;
                endY = h - 1;
            } else {//{1, 0}
                startX = 0;
                startY = 0;
                endX = w - 1;
                endY = 0;
            }

            while (check(startX, startY)) {
                try {
                    BoardGameState gameState = checkLine(startX, startY, endX, endY, dir, side);
                    if (gameState.state() == State.WIN) {
                        return gameState;
                    } else if (gameState.state() == State.CONTINUE) {
                        isDRAW = false;
                    }
                    point += gameState.point();
                } catch (WrongOperationInBoardException e) {
                    throw new RuntimeException(e);
                }

                if (dir[0] == -1 && dir[1] == 1) {
                    if (startX < (w - 1)) {
                        startX++;
                        endX++;
                    } else {
                        startY++;
                        endY++;
                    }
                } else if (dir[0] == 1 && dir[1] == 1) {
                    if (startX > 0) {
                        startX--;
                        endX--;
                    } else {
                        startY++;
                        endY++;
                    }
                } else if (dir[0] == 0 && dir[1] == 1) {
                    startX++;
                } else {//{1, 0}
                    startY++;
                }
            }
        }

        if (isDRAW) {
            point = 0;
        }else {
            Side tmpSide = timesFourOnLine > 0 ? Side.POSITIVE : Side.NEGATIVE;
            BoardGameState tmpWin = new BoardGameState(State.WIN, tmpSide, tmpSide == Side.POSITIVE ? MAX_POINT : MIN_POINT);
            if (timesFourOnLine >= 2 || timesFourOnLine <= -2){//某一方拥有两个冲四
                return tmpWin;
            }else {
                if (timesFourOnLine != 0 && ((tmpSide == Side.POSITIVE ? p_timesThreeOnLine : n_timesThreeOnLine) > 0)){//某一方拥有冲四和活三
                    return tmpWin;
                }else {
                    Side nextSide = Side.nextSide(side);
                    BoardGameState sideWin = new BoardGameState(State.WIN, side, side == Side.POSITIVE ? MAX_POINT : MIN_POINT);
                    if (p_timesThreeOnLine > 0 && n_timesThreeOnLine > 0){//双方都有活三
                        return sideWin;
                    }else if ((tmpSide == Side.POSITIVE ? p_timesThreeOnLine : n_timesThreeOnLine) > 0 ){//先手方有活三
                        return sideWin;
                    }else if ((nextSide == Side.POSITIVE ? p_timesThreeOnLine : n_timesThreeOnLine) > 2){//后手方有两个活三
                        return new BoardGameState(State.WIN, nextSide, nextSide == Side.POSITIVE ? MAX_POINT : MIN_POINT);
                    }
                }
            }
            point += 1024 * timesFourOnLine;
            point += 256 * (p_timesThreeOnLine - n_timesThreeOnLine);
        }
        return new BoardGameState(isDRAW ? State.DRAW : State.CONTINUE, Side.NONE, point);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BoardGameState getBoardGameStateLast(int x, int y, byte chess) throws WrongOperationInBoardException {
        for (int[] dir : DIRECTIONS) {
            int startX = x - dir[0] * 4;
            int startY = y - dir[1] * 4;
            int endX = x + dir[0] * 4;
            int endY = y + dir[1] * 4;
            BoardGameState tmp = checkLine(startX, startY, endX, endY, dir, Side.NONE);
            if (tmp.state() == State.WIN) {
                return tmp;
            }
        }
        return new BoardGameState(State.CONTINUE, Side.NONE, 0);
    }

    private int[] fixMin(int x, int y, int[] dir) {
        boolean b = x < y;
        int tmp = b ? x : y;
        if (b) {
            if (dir[0] > 0) {
                tmp *= -1;
            }
        } else {
            if (dir[1] > 0) {
                tmp *= -1;
            }
        }
        x += tmp * dir[0];
        y += tmp * dir[1];
        return new int[]{x, y};
    }

    private int[] fixMax(int x, int y, int[] dir) {
        int t1 = x - getWidth() + 1;
        int t2 = y - getHeight() + 1;
        boolean b = t1 > t2;
        int tmp = b ? t1 : t2;
        if (b) {
            if (dir[0] > 0) {
                tmp *= -1;
            }
        } else {
            if (dir[1] > 0) {
                tmp *= -1;
            }
        }
        x += tmp * dir[0];
        y += tmp * dir[1];
        return new int[]{x, y};
    }

    private int[] fix(int x, int y, int[] dir) {
        int w = getWidth();
        int h = getHeight();
        int[] tmp;
        if (Math.min(x, y) < 0) {
            tmp = fixMin(x, y, dir);
            x = tmp[0];
            y = tmp[1];
        }
        if (x >= w || y >= h) {
            tmp = fixMax(x, y, dir);
            x = tmp[0];
            y = tmp[1];
        }
        return new int[]{x, y};
    }

    protected boolean check(int x, int y) {
        return x >= 0 && x < getWidth() && y >= 0 && y < getHeight();
    }

    /**
     * 检查五元组
     *
     * @param startX 起始坐标
     * @param startY 起始坐标
     * @param dir    方向
     * @return <p>如果此条线上已出现五连,则返回胜利并包含胜利方</p>
     * <p>如果此条线上仍有可能出现五连,则返回继续;否则,返回平局</p>
     */
    private BoardGameState checkLineFive(int startX, int startY, int[] dir, Side side) throws WrongOperationInBoardException {
        Side tmp = null;
        int times = 0;

        boolean checkWin = true;

        int x = startX;
        int y = startY;
        for (int i = 0; i < 5; i++) {
            byte chess = getChessInBoard(x, y);
            if (chess == EMPTY) {
                if (i != 0 && i != 4){
                    checkWin = false;
                }
            } else {
                Side s = chess > 0 ? Side.POSITIVE : Side.NEGATIVE;
                if (tmp == null) {
                    tmp = s;
                    times = 1;
                } else {
                    if (tmp == s) {
                        ++times;
                    } else {
                        return new BoardGameState(State.DRAW, Side.NONE, 0);
                    }
                }
            }

            x += dir[0];
            y += dir[1];
        }

        BoardGameState tmpWin = new BoardGameState(State.WIN, tmp, tmp == Side.POSITIVE ? MAX_POINT : MIN_POINT);
        if (times == 5){
            return tmpWin;
        }
        if (side != Side.NONE && checkWin) {
            if (times == 4){//探测到冲四
                if (side == tmp){
                    return tmpWin;
                }else {
                    if (timesFourOnLine == 0 || ((timesFourOnLine > 0) == (tmp == Side.POSITIVE))){
                        timesFourOnLine += tmp == Side.POSITIVE ? 1 : -1;
                    }else {//双方都有冲四
                        return new BoardGameState(State.WIN, side, side == Side.POSITIVE ? MAX_POINT : MIN_POINT);
                    }
                }
            }else if (times == 3){
                boolean b = false;

                x = startX - dir[0];
                y = startY - dir[1];
                if (check(x, y)){
                    b = Side.getSideByChess(getChessInBoard(x, y)) != Side.nextSide(tmp);
                }

                x = startX + 5*dir[0];
                y = startY + 5*dir[1];
                if (!b && check(x, y)){
                    b = Side.getSideByChess(getChessInBoard(x, y)) != Side.nextSide(tmp);
                }

                if (b){//探测到活三
                    if (tmp == Side.POSITIVE){
                        ++p_timesThreeOnLine;
                    }else {
                        ++n_timesThreeOnLine;
                    }
                }
            }
        }

        return new BoardGameState(State.CONTINUE, Side.NONE, tmp == Side.POSITIVE ? LINE_POINT[times] : -LINE_POINT[times]);
    }

    /**
     * 检查棋盘上的一条线
     *
     * @param startX 起始坐标
     * @param startY 起始坐标
     * @param endX   结束坐标
     * @param endY   结束坐标
     * @param dir    方向
     * @param side   此时轮到某一方
     * @return <p>如果此条线上已出现五连,则返回胜利并包含胜利方</p>
     * <p>如果此条线上仍有可能出现五连,则返回继续;否则,返回平局</p>
     */
    protected BoardGameState checkLine(int startX, int startY, int endX, int endY, int[] dir, Side side) throws WrongOperationInBoardException {
        int point = 0;

        int[] tmp;

        tmp = fix(startX, startY, dir);
        startX = tmp[0];
        startY = tmp[1];

        tmp = fix(endX, endY, dir);
        endX = tmp[0];
        endY = tmp[1];

        int x = startX;
        int y = startY;

        int tmpX = x + 4 * dir[0];
        int tmpY = y + 4 * dir[1];

        boolean isDraw = true;
        while (check(x, y) && check(tmpX, tmpY) &&
                (dir[0] > 0 ? (x <= endX) : (x >= endX)) && (dir[1] > 0 ? (y <= endY) : (y >= endY)) &&
                (dir[0] > 0 ? (tmpX <= endX) : (tmpX >= endX)) && (dir[1] > 0 ? (tmpY <= endY) : (tmpY >= endY))) {
            BoardGameState boardGameState = checkLineFive(x, y, dir, side);
            if (boardGameState.state() == State.WIN) {
                return boardGameState;
            } else if (boardGameState.state() == State.CONTINUE) {
                isDraw = false;
                point += boardGameState.point();
            }

            x += dir[0];
            y += dir[1];
            tmpX += dir[0];
            tmpY += dir[1];
        }

        return new BoardGameState(isDraw ? State.DRAW : State.CONTINUE, Side.NONE, point);
    }
}
