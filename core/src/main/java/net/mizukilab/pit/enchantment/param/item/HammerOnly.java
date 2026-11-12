package net.mizukilab.pit.enchantment.param.item;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 仅可应用于神话之锤（PDC internal name = mythic_hammer）。
 *
 * 标记在 HammerEnchantBase 上，100 条锤附魔均继承自该基类，
 * 因此必须声明 {@link Inherited}，否则具体子类上的 isAnnotationPresent 恒为 false。
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface HammerOnly {
}