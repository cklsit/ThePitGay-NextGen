package net.mizukilab.pit.enchantment.type.hammer

/**
 * 锤附魔的结算类型。引擎只自动结算可数据化的四类，其余为特殊逻辑（暂不自动生效）。
 */
enum class HammerEffect {
    /** 增伤：valueAt(level) 为百分比，C4 组合计封顶 +60%。 */
    DAMAGE_PCT,
    /** 自身减伤：valueAt(level) 为百分比，C1 组合计封顶 35%。 */
    DAMAGE_REDUCTION,
    /** 回复生命值：valueAt(level) 为 ❤（0.5 = 1 点生命值）。 */
    HEAL,
    /** 特殊逻辑：由后续专用处理器实现，引擎不自动结算。 */
    SPECIAL
}
