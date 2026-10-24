package com.sablednah.apocalypsetoolkit.explosive;

import java.util.UUID;

import com.sablednah.apocalypsetoolkit.ModRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/** A C4 charge's memory: whose detonator sets it off. */
public class C4BlockEntity extends BlockEntity {
    private @Nullable UUID owner;

    public C4BlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistry.C4_BE.get(), pos, state);
    }

    public @Nullable UUID owner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
        setChanged();
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        owner = input.getString("owner").map(UUID::fromString).orElse(null);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (owner != null) output.putString("owner", owner.toString());
    }
}
