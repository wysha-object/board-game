package cn.com.wysha.board.exception;

/**
 * 错误的坐标/错误的棋子
 */
public class WrongOperationInBoardException extends Exception {
    public WrongOperationInBoardException() {
        super("Wrong operation !");
    }
}
