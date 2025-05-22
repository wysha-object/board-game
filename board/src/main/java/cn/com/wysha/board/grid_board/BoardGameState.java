package cn.com.wysha.board.grid_board;

/**
 * <p>游戏状态
 * <p>若没有得分,则置0
 *
 * @param state  棋盘状态
 * @param winner <p>如果有一方赢了,其设置为赢的那一方
 *
 *               <p>如果为平局或者没有任何一方获胜,则设置为{@code Side.NONE}
 * @param point  得分
 * @see State
 * @see Side
 */
public record BoardGameState(State state, Side winner, int point) {
}
