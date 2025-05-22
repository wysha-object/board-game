package cn.com.wysha.board.grid_board.put_chess.gomoku;

import cn.com.wysha.board.ai.put_chess.PutChessAI;
import cn.com.wysha.board.exception.WrongOperationInBoardException;
import cn.com.wysha.board.grid_board.AbstractGridBoard;
import cn.com.wysha.board.grid_board.BoardGameState;
import cn.com.wysha.board.grid_board.Side;
import cn.com.wysha.board.grid_board.State;
import cn.com.wysha.board.grid_board.put_chess.AbstractPutChessGridBoard;
import cn.com.wysha.config_Manager.manger.Manager;
import cn.com.wysha.config_Manager.manger.ini.IniManager;

import java.util.Scanner;

public class GomokuMain {
    public static void main(String[] args) {
        Manager manager = new IniManager();
        try {
            manager.readStaticConfig();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Gomoku gomoku = new Gomoku(15, 15);
        PutChessAI ai = new PutChessAI(gomoku, 6, 4, AbstractPutChessGridBoard.P_CHESS, AbstractPutChessGridBoard.N_CHESS);
        BoardGameState gameState;
        do {
            try {
                printBoard(gomoku);
            } catch (WrongOperationInBoardException e) {
                throw new RuntimeException(e);
            }
            Scanner scanner = new Scanner(System.in);
            int x = scanner.nextInt();
            int y = scanner.nextInt();
            try {
                gameState = gomoku.putChess(x, y, AbstractPutChessGridBoard.P_CHESS);
            } catch (WrongOperationInBoardException e) {
                throw new RuntimeException(e);
            }
            if (gameState.state() == State.WIN || gameState.state() == State.DRAW) {
                break;
            }
            ai.doNext(Side.NEGATIVE);
            gameState = gomoku.getBoardGameState();
        } while ((gameState.state() != State.WIN) && (gameState.state() != State.DRAW));
        ai.saveCache();
        try {
            manager.writeStaticConfig();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        System.exit(0);
    }

    public static void printBoard(AbstractGridBoard abstractGridBoard) throws WrongOperationInBoardException {
        System.out.printf("%4d ", -1);
        for (int j = 0; j < abstractGridBoard.getWidth(); j++) {
            System.out.printf("%4d ", j);
        }
        System.out.print("\n\n");
        for (int i = 0; i < abstractGridBoard.getHeight(); i++) {
            System.out.printf("%4d ", i);
            for (int j = 0; j < abstractGridBoard.getWidth(); j++) {
                Side side = Side.getSideByChess(abstractGridBoard.getChessInBoard(j, i));
                char c = side == Side.NONE ? '.' :
                        side == Side.POSITIVE ? 'O' : 'X';
                System.out.printf("%4s ", c);
            }
            System.out.print("\n\n");
        }
    }
}
