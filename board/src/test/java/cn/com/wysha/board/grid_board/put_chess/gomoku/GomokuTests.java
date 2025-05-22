package cn.com.wysha.board.grid_board.put_chess.gomoku;

import cn.com.wysha.board.ai.put_chess.PutChessAI;
import cn.com.wysha.board.exception.WrongOperationInBoardException;
import cn.com.wysha.board.grid_board.BoardGameState;
import cn.com.wysha.board.grid_board.Side;
import cn.com.wysha.board.grid_board.State;
import cn.com.wysha.board.grid_board.put_chess.AbstractPutChessGridBoard;
import org.junit.Test;

import java.util.Arrays;

public class GomokuTests {

    /**
     * 胜利判断测试
     */
    @Test(timeout = 10)
    public void winnerTest() throws WrongOperationInBoardException {
        Gomoku gomoku = new Gomoku(19, 19);
        BoardGameState boardGameState;
        for (int i = 0; i < 5; i++) {
            if (i == 2) continue;
            boardGameState = gomoku.putChess(i, i, AbstractPutChessGridBoard.P_CHESS);
            if (boardGameState.state() != State.CONTINUE) {
                throw new RuntimeException();
            }
        }
        boardGameState = gomoku.putChess(2, 2, AbstractPutChessGridBoard.P_CHESS);
        if (boardGameState.state() != State.WIN) {
            throw new RuntimeException();
        }
        if (boardGameState.winner() != Side.POSITIVE) {
            throw new RuntimeException();
        }
        boardGameState = gomoku.getBoardGameState();
        if (boardGameState.state() != State.WIN) {
            throw new RuntimeException();
        }
        if (boardGameState.winner() != Side.POSITIVE) {
            throw new RuntimeException();
        }
    }

    private int checkLineTest(int startX, int startY, int[] dir, int next, boolean side, boolean otherTest) throws WrongOperationInBoardException {
        Gomoku gomoku = new Gomoku(19, 19);

        byte chess1;
        byte chess2;
        if (side) {
            chess1 = AbstractPutChessGridBoard.P_CHESS;
            chess2 = AbstractPutChessGridBoard.N_CHESS;
        } else {
            chess1 = AbstractPutChessGridBoard.N_CHESS;
            chess2 = AbstractPutChessGridBoard.P_CHESS;
        }

        int endX = startX + dir[0] * 19;
        int endY = startY + dir[1] * 19;
        for (int i = 0; i < 3; i++) {
            int x = startX + i * next * dir[0];
            int y = startY + i * next * dir[1];
            if (gomoku.check(x, y)) {
                gomoku.setChessInBoard(x, y, chess1);
            } else {
                break;
            }
        }
        if (otherTest) {
            int x = startX + (2 * next + 1) * dir[0];
            int y = startY + (2 * next + 1) * dir[1];
            if (gomoku.check(x, y)) {
                gomoku.setChessInBoard(x, y, chess2);
            }
        }

        int a = gomoku.checkLine(startX, startY, endX, endY, dir, Side.NONE).point();
        int b = gomoku.checkLine(endX, endY, startX, startY, new int[]{-dir[0], -dir[1]}, Side.NONE).point();

        if (a != b) {
            System.out.println(startX);
            System.out.println(startY);
            System.out.println(a);
            System.out.println(b);
            throw new RuntimeException();
        }

        return a;
    }

    private void checkLineTest(int x, int y, int[] dir, int next) throws WrongOperationInBoardException {
        boolean other = false;
        for (int i = 0; i < 2; i++) {
            dir = Arrays.copyOf(dir, 2);
            int a, b;

            a = checkLineTest(x, y, dir, next, true, other);
            b = checkLineTest(x, y, dir, next, false, other);
            if (a != -b) {
                throw new RuntimeException();
            }

            other = !other;
        }
    }

    /**
     * 对称情景的一致性测试
     */
    @Test(timeout = 100)
    public void checkLineTest() throws WrongOperationInBoardException {
        int[][] testData = new int[][]{{-1, 1}, {1, 1}, {0, 1}, {1, 0}};
        for (int[] test : testData) {
            for (int i = 0; i < 19; i++) {
                for (int j = 0; j < 19; j++) {
                    for (int k = 0; k < 5; k++) {
                        checkLineTest(i, j, test, k);
                    }
                }
            }
        }
    }


    /**
     * getPoint有效性测试
     * AI稳定性测试
     */
    @Test
    public void aiTest() throws WrongOperationInBoardException {
        Gomoku gomoku = new Gomoku(15, 15);
        PutChessAI ai1 = new PutChessAI(gomoku, 6, 3, AbstractPutChessGridBoard.P_CHESS, AbstractPutChessGridBoard.N_CHESS);
        PutChessAI ai2 = new PutChessAI(gomoku, 6, 3, AbstractPutChessGridBoard.P_CHESS, AbstractPutChessGridBoard.N_CHESS);
        BoardGameState gameState;
        Side side1 = Side.POSITIVE;
        Side side2 = Side.NEGATIVE;
        do {
            ai1.doNext(side1);
            GomokuMain.printBoard(gomoku);

            gameState = gomoku.getBoardGameState();
            if (gameState.state() == State.WIN || gameState.state() == State.DRAW){
                break;
            }

            ai2.doNext(side2);
            GomokuMain.printBoard(gomoku);

            gameState = gomoku.getBoardGameState();
        } while ((gameState.state() != State.WIN) && (gameState.state() != State.DRAW));
    }
}
