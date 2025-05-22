package cn.com.wysha.board.ai;

import java.io.Serial;
import java.io.Serializable;

public class DefaultOperation implements Serializable {
    @Serial
    private static final long serialVersionUID = 0L;

    /**
     * 深度
     */
    private final int height;

    /**
     * 最佳操作得分
     */
    private final int point;

    /**
     * 最佳操作X轴坐标
     */
    private final int d_s;

    /**
     * 最佳操作Y轴坐标
     */
    private final int d_y;

    public DefaultOperation(int height, int point, int dS, int dY) {
        this.height = height;
        this.point = point;
        d_s = dS;
        d_y = dY;
    }

    public int getHeight() {
        return height;
    }

    public int getPoint() {
        return point;
    }

    public int getD_x() {
        return d_s;
    }

    public int getD_y() {
        return d_y;
    }
}
