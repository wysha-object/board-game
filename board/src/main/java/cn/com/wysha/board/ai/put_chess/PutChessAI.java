package cn.com.wysha.board.ai.put_chess;

import cn.com.wysha.board.ai.AbstractAI;
import cn.com.wysha.board.ai.DefaultOperation;
import cn.com.wysha.board.exception.WrongOperationInBoardException;
import cn.com.wysha.board.grid_board.*;
import cn.com.wysha.board.grid_board.put_chess.AbstractPutChessGridBoard;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

public class PutChessAI extends AbstractAI {

    /**
     * 正方棋子
     */
    private final byte chess_POSITIVE;
    /**
     * 反方棋子
     */
    private final byte chess_NEGATIVE;
    private volatile boolean stopFlag = true;

    /**
     * {@inheritDoc}
     */
    public PutChessAI(AbstractPutChessGridBoard gridBoard, int maxDeepin, int minDeepin, byte chessPositive, byte chessNegative) {
        super(gridBoard, maxDeepin, minDeepin);

        chess_POSITIVE = chessPositive;
        chess_NEGATIVE = chessNegative;
    }

    private void updateCurrentDeepin(int positionNum){
        AbstractPutChessGridBoard gridBoard = getGridBoard();
        int s = gridBoard.getWidth() * gridBoard.getHeight();
        currentDeepin = minDeepin;
        while (currentDeepin < maxDeepin){
            s >>= 1;
            if (positionNum <= s){
                ++currentDeepin;
            }else {
                break;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected AbstractPutChessGridBoard getGridBoard() {
        return (AbstractPutChessGridBoard) super.getGridBoard();
    }

    /**
     * {@inheritDoc}
     */
    private byte getChessBySide(Side side) {
        if (side == Side.POSITIVE) return chess_POSITIVE;
        if (side == Side.NEGATIVE) return chess_NEGATIVE;
        return AbstractGridBoard.EMPTY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DefaultOperation getNext(Side side) {
        try {
            stopFlag = false;
            AbstractPutChessGridBoard gridBoard = getGridBoard();
            DefaultOperation rs;
            boolean b = side == Side.POSITIVE;
            Side nextSide = Side.nextSide(side);

            BoardPositionState tmp_S = new BoardPositionState(gridBoard.getBoardEqualsMode(), gridBoard, side);

            int rs_point = 0;
            int rs_x = -1;
            int rs_y = -1;

            CalData data = new CalData(AbstractGridBoard.MIN_POINT, AbstractGridBoard.MAX_POINT);

            boolean isDRAW = true;

            LinkedList<CalWorker> calWorkers = new LinkedList<>();
            LinkedList<FutureTask<Integer>> threads = new LinkedList<>();

            PriorityQueue<CalWorker> priorityQueue = new PriorityQueue<>();
            for (int c_x = 0; c_x < gridBoard.getWidth(); c_x++) {
                for (int c_y = 0; c_y < gridBoard.getHeight(); c_y++) {
                    if (!gridBoard.isNotAllowPutChess(c_x, c_y, getChessBySide(side))) {
                        isDRAW = false;
                        CalWorker calWorker = new CalWorker(c_x, c_y, data, nextSide, gridBoard);
                        priorityQueue.add(calWorker);
                    }
                }
            }

            updateCurrentDeepin(priorityQueue.size());
            for (int i = maxDeepin; i >= currentDeepin; --i) {
                DefaultOperation tmp_d = getOperation(tmp_S, i);
                if (tmp_d != null) {
                    int x_ = gridBoard.getWidth() - 1 - tmp_d.getD_x();
                    int y_ = gridBoard.getHeight() - 1 - tmp_d.getD_y();

                    return switch (tmp_S.getLast_BoardEqualsMode()) {
                        case FULL -> tmp_d;
                        case UP_DOWN -> new DefaultOperation(tmp_d.getHeight(), tmp_d.getPoint(), tmp_d.getD_x(), y_);
                        case LEFT_RIGHT -> new DefaultOperation(tmp_d.getHeight(), tmp_d.getPoint(), x_, tmp_d.getD_y());
                        case ALL -> new DefaultOperation(tmp_d.getHeight(), tmp_d.getPoint(), x_, y_);
                    };
                }
            }

            while (!priorityQueue.isEmpty()){
                CalWorker calWorker = priorityQueue.poll();
                FutureTask<Integer> futureTask = new FutureTask<>(calWorker);
                EXECUTOR_SERVICE.execute(futureTask);
                calWorkers.add(calWorker);
                threads.add(futureTask);
            }

            Iterator<CalWorker> calIterator = calWorkers.iterator();
            Iterator<FutureTask<Integer>> iterator = threads.iterator();
            while (iterator.hasNext()) {
                FutureTask<Integer> futureTask = iterator.next();
                CalWorker calWorker = calIterator.next();
                iterator.remove();
                calIterator.remove();

                int tmp = futureTask.get();
                if (b ? tmp > rs_point : tmp < rs_point || rs_x == -1) {
                    rs_point = tmp;
                    rs_x = calWorker.l_x;
                    rs_y = calWorker.l_y;
                    if (b) {
                        data.alpha = tmp;
                    } else {
                        data.beta = tmp;
                    }
                    if (data.alpha >= data.beta) {
                        break;
                    }
                }
            }

            stopFlag = true;

            while (iterator.hasNext()) {
                FutureTask<Integer> futureTask = iterator.next();
                futureTask.get();
            }

            rs_point = isDRAW ? 0 : rs_point;
            rs = new DefaultOperation(currentDeepin, rs_point, rs_x, rs_y);

            putOperation(tmp_S, rs);

            return rs;
        } catch (WrongOperationInBoardException | ExecutionException | InterruptedException e) {
            stopFlag = true;
            throw new RuntimeException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void doNext(Side side) {
        DefaultOperation defaultOperation = getNext(side);
        if (defaultOperation.getD_x() < 0 || defaultOperation.getD_y() < 0) return;
        try {
            getGridBoard().putChess(defaultOperation.getD_x(), defaultOperation.getD_y(), getChessBySide(side));
        } catch (WrongOperationInBoardException e) {
            throw new RuntimeException(e);
        }
    }

    protected static final class CalData {
        /**
         * 反方必须反驳的值
         */
        public volatile int alpha;
        /**
         * 正方必须反驳的值
         */
        public volatile int beta;

        public CalData(int alpha, int beta) {
            this.alpha = alpha;
            this.beta = beta;
        }
    }

    protected class CalWorker implements Callable<Integer>, Comparable<CalWorker> {
        private final int l_x;
        private final int l_y;
        private final CalData data;
        private final Side side;
        private AbstractPutChessGridBoard gridBoard;

        public CalWorker(int l_x, int l_y, CalData data, Side side, AbstractPutChessGridBoard gridBoard) {
            this.l_x = l_x;
            this.l_y = l_y;
            this.data = data;
            this.side = side;
            this.gridBoard = gridBoard;
        }

        private Integer cal(BoardPositionState boardPositionState, int l_x, int l_y, CalData calData, Side side, int height) {
            if (stopFlag) {
                return null;
            }

            try {
                int rs;
                boolean b = side == Side.POSITIVE;
                Side nextSide = Side.nextSide(side);

                BoardGameState boardGameState = gridBoard.putChess(l_x, l_y, getChessBySide(nextSide));
                if (boardGameState.state() == State.WIN) {
                    gridBoard.undo(boardPositionState, l_x, l_y);
                    int tmp = (currentDeepin - height - 1);
                    return b ? AbstractGridBoard.MIN_POINT + tmp : AbstractGridBoard.MAX_POINT - tmp;
                } else if (boardGameState.state() == State.DRAW) {
                    gridBoard.undo(boardPositionState, l_x, l_y);
                    return 0;
                }

                BoardPositionState tmp_S = new BoardPositionState(gridBoard.getBoardEqualsMode(), gridBoard, side);

                if (height >= MIN_SAVE_HEIGHT) {
                    DefaultOperation tmp_d = getOperation(tmp_S, height);
                    if (tmp_d != null) {
                        gridBoard.undo(boardPositionState, l_x, l_y);
                        return tmp_d.getPoint();
                    }
                }

                //是否缓存,如果发生过剪枝,则不进行缓存
                boolean save = true;

                int rs_x = -1, rs_y = -1;
                if (height <= 0) {
                    rs = gridBoard.getPoint(side);
                    int tmp = (currentDeepin - height);
                    if (rs == AbstractGridBoard.MAX_POINT){
                        rs -= tmp;
                    }else if (rs == AbstractGridBoard.MIN_POINT){
                        rs += tmp;
                    }
                } else {
                    CalData nextData = new CalData(calData.alpha, calData.beta);

                    rs = 0;

                    boolean isDRAW = true;

                    PriorityQueue<CalWorker> priorityQueue = new PriorityQueue<>();
                    for (int c_x = 0; c_x < gridBoard.getWidth(); c_x++) {
                        for (int c_y = 0; c_y < gridBoard.getHeight(); c_y++) {
                            if (!gridBoard.isNotAllowPutChess(c_x, c_y, getChessBySide(side))) {
                                isDRAW = false;
                                CalWorker calWorker = new CalWorker(c_x, c_y, nextData, nextSide, gridBoard);
                                priorityQueue.add(calWorker);
                            }
                        }
                    }

                    while (!priorityQueue.isEmpty() && !stopFlag){
                        CalWorker calWorker = priorityQueue.poll();
                        Integer integer = cal(tmp_S, calWorker.l_x, calWorker.l_y, nextData, nextSide, height - 1);
                        if (stopFlag) {
                            break;
                        }
                        int tmp = integer;

                        if (b) {
                            nextData.beta = calData.beta;
                            if (calData.alpha > nextData.alpha){
                                nextData.alpha = calData.alpha;
                            }
                        } else {
                            nextData.alpha = calData.alpha;
                            if (calData.beta < nextData.beta){
                                nextData.beta = calData.beta;
                            }
                        }
                        if (b ? tmp > rs : tmp < rs || rs_x == -1) {
                            rs = tmp;
                            rs_x = calWorker.l_x;
                            rs_y = calWorker.l_y;
                            if (b) {
                                if (tmp > nextData.alpha){
                                    nextData.alpha = tmp;
                                }
                                if (nextData.alpha >= calData.beta) {
                                    save = false;
                                    break;
                                }
                            } else {
                                if (tmp < nextData.beta){
                                    nextData.beta = tmp;
                                }
                                if (calData.alpha >= nextData.beta) {
                                    save = false;
                                    break;
                                }
                            }
                        }

                    }

                    if (stopFlag) {
                        return null;
                    }

                    rs = isDRAW ? 0 : rs;
                }

                if (save && height >= MIN_SAVE_HEIGHT) {
                    putOperation(tmp_S, new DefaultOperation(height, rs, rs_x, rs_y));
                }

                gridBoard.undo(boardPositionState, l_x, l_y);
                return rs;
            } catch (WrongOperationInBoardException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public Integer call() {
            if (stopFlag) {
                return null;
            }
            gridBoard = (AbstractPutChessGridBoard) gridBoard.copy();
            return cal(new BoardPositionState(gridBoard.getBoardEqualsMode(), gridBoard, side), l_x, l_y, data, side, currentDeepin - 1);
        }

        @Override
        public int compareTo(CalWorker o) {
            return gridBoard.compare(l_x, l_y, o.l_x, o.l_y);
        }
    }
}
