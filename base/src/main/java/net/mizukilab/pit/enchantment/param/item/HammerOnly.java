package net.mizukilab.pit.enchantment.param.item;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 仅可应用于神话之锤（PDC internal name = mythic_hammer）。
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface HammerOnly {
}
