package com.sablednah.apocalypsetoolkit.turret;

import com.sablednah.apocalypsetoolkit.ModRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A sentry turret: placed, it belongs to whoever placed it and guards the ground around it ({@link SentryBlockEntity}).
 * Right-click opens its screen (ammunition, repairs, what it shoots at). Its head turns smoothly to its target
 * ({@code SentryRenderer}); {@link #FACING} is only which way it first faces.
 */
public class SentryBlock extends Block implements EntityBlock {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    /** Never true in the world: it names the head's model, which {@code SentryRenderer} draws turned to its aim. */
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty HEAD =
            net.minecraft.world.level.block.state.properties.BooleanProperty.create("head");
    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 14, 13);

    public SentryBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(HEAD, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HEAD);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer instanceof Player p && level.getBlockEntity(pos) instanceof SentryBlockEntity be) be.setOwner(p);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof SentryBlockEntity be) player.openMenu(be);
        return InteractionResult.SUCCESS;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SentryBlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != ModRegistry.SENTRY_BE.get()) return null;
        return level.isClientSide() ? (BlockEntityTicker<T>) (BlockEntityTicker<SentryBlockEntity>) SentryBlockEntity::clientTick
                : (BlockEntityTicker<T>) (BlockEntityTicker<SentryBlockEntity>) SentryBlockEntity::serverTick;
    }
}
