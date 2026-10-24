package com.sablednah.apocalypsetoolkit.explosive;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A C4 charge: stuck to a floor, wall or ceiling like a button, it belongs to whoever placed it and goes off when
 * their detonator says so ({@link DetonatorItem}), or a few ticks after another blast reaches it. Broken by hand it
 * drops itself, safe.
 */
public class C4Block extends FaceAttachedHorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<C4Block> CODEC = simpleCodec(C4Block::new);

    private static final VoxelShape FLOOR = Block.box(4, 0, 3, 12, 3, 13), CEILING = Block.box(4, 13, 3, 12, 16, 13);
    private static final VoxelShape NORTH = Block.box(4, 3, 13, 12, 13, 16), SOUTH = Block.box(4, 3, 0, 12, 13, 3),
            EAST = Block.box(0, 3, 4, 3, 13, 12), WEST = Block.box(13, 3, 4, 16, 13, 12);

    public C4Block(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(FACE, net.minecraft.world.level.block.state.properties.AttachFace.FLOOR));
    }

    @Override
    protected MapCodec<C4Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FACE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return switch (state.getValue(FACE)) {
            case FLOOR -> FLOOR;
            case CEILING -> CEILING;
            case WALL -> switch (state.getValue(FACING)) {
                case SOUTH -> SOUTH;
                case EAST -> EAST;
                case WEST -> WEST;
                default -> NORTH;
            };
        };
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer instanceof Player p && level.getBlockEntity(pos) instanceof C4BlockEntity be) be.setOwner(p.getUUID());
    }

    @Override
    public void wasExploded(ServerLevel level, BlockPos pos, Explosion explosion) {
        Blasts.queue(level, pos, 4 + level.getRandom().nextInt(4), null);
    }

    @Override
    public boolean dropFromExplosion(Explosion explosion) {
        return false;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new C4BlockEntity(pos, state);
    }
}
