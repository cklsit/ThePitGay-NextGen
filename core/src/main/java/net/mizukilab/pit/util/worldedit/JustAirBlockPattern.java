package net.mizukilab.pit.util.worldedit;

import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.pattern.Pattern;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;

/**
 * 只替换空气方块的花纹（WorldEdit 7 API）。
 *
 * <p>1.8 时代实现基于 WorldEdit 6 的 {@code patterns.Pattern} + {@code Vector}；
 * WorldEdit 7 的接口是 {@code function.pattern.Pattern#applyBlock(BlockVector3)}。</p>
 */
public class JustAirBlockPattern implements Pattern {

    private final BaseBlock block;

    public JustAirBlockPattern(BaseBlock block) {
        this.block = block;
    }

    public BaseBlock getBlock() {
        return this.block;
    }

    @Override
    public BaseBlock applyBlock(BlockVector3 position) {
        return this.block;
    }

    @Override
    public boolean apply(Extent extent, BlockVector3 setPosition, BlockVector3 getPosition) throws WorldEditException {
        if (!extent.getBlock(getPosition).isAir()) {
            return false;
        }
        extent.setBlock(setPosition, this.block);
        return true;
    }
}
