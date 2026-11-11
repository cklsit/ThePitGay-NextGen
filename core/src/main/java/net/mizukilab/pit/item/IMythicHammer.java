package net.mizukilab.pit.item;

/**
 * 神话之锤标记接口。
 *
 * <p>hammer 专属附魔体系（main 分支的 112 个 hammer 类）尚未随 NextGen 迁回，
 * 这里只保留类型判定所需的最小契约，等该特性迁回时再补齐 hammer 相关方法。</p>
 *
 * @see IMythicSword
 */
public interface IMythicHammer {

    double getItemDamage();
}
