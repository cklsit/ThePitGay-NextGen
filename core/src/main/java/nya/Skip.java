package nya;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 标记注解占位。
 *
 * <p>NextGen 基线依赖授权库提供的 {@code nya.Skip} 标记（140+ 个类上有 {@code @Skip}）。
 * 该授权库整体混淆且会让 Paper 的插件重映射（PluginRemapper）崩溃，
 * 已不再随插件打包，这里内置一份等价定义保持源码兼容。</p>
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Skip {
}
