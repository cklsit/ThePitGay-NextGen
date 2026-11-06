package cn.charlotte.pit.compat.math;

/**
 * 1.8 的 {@code net.minecraft.server.MathHelper} 等价实现。
 * <p>
 * 原方法使用查表近似，这里改用 {@link Math} 的双精度实现再收敛为 float，
 * 精度更高且不依赖 NMS 版本。
 */
public final class MathHelper {

    private MathHelper() {
    }

    public static float sin(float value) {
        return (float) StrictMath.sin(value);
    }

    public static float cos(float value) {
        return (float) StrictMath.cos(value);
    }

    public static float sqrt(float value) {
        return (float) StrictMath.sqrt(value);
    }

    public static float sqrt(double value) {
        return (float) StrictMath.sqrt(value);
    }

    public static int floor(float value) {
        int i = (int) value;
        return value < (float) i ? i - 1 : i;
    }

    public static int floor(double value) {
        int i = (int) value;
        return value < (double) i ? i - 1 : i;
    }

    /**
     * 对应 1.8 {@code MathHelper.g(float)}：向上取整。
     */
    public static int g(float value) {
        int i = (int) value;
        return value > (float) i ? i + 1 : i;
    }
}
