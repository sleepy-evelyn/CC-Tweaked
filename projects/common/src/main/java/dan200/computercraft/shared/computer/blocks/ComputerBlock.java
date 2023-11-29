// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.computer.blocks;

import com.google.common.collect.ImmutableMap;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ComputerState;
import dan200.computercraft.shared.computer.items.ComputerItemFactory;
import dan200.computercraft.shared.platform.RegistryEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Map;

public class ComputerBlock<T extends ComputerBlockEntity> extends AbstractComputerBlock<T> {
    public static final EnumProperty<ComputerState> STATE = EnumProperty.create("state", ComputerState.class);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final Map<Direction, VoxelShape> DEFAULT_SHAPES;

    public ComputerBlock(Properties settings, ComputerFamily family, RegistryEntry<BlockEntityType<T>> type) {
        super(settings, family, type);
        registerDefaultState(defaultBlockState()
            .setValue(FACING, Direction.NORTH)
            .setValue(STATE, ComputerState.OFF)
        );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, STATE);
    }

    @Override
    @Deprecated
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return DEFAULT_SHAPES.getOrDefault(state.getValue(FACING), DEFAULT_SHAPES.get(Direction.NORTH));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext placement) {
        return defaultBlockState().setValue(FACING, placement.getHorizontalDirection().getOpposite());
    }

    @Override
    protected ItemStack getItem(AbstractComputerBlockEntity tile) {
        return tile instanceof ComputerBlockEntity ? ComputerItemFactory.create((ComputerBlockEntity) tile) : ItemStack.EMPTY;
    }

    static {
        var builder = ImmutableMap.<Direction, VoxelShape>builder();

        builder.put(Direction.NORTH, Block.box(1, 1, 3, 15, 16, 15));
        builder.put(Direction.SOUTH, Block.box(1, 1, 1, 15, 16, 13));
        builder.put(Direction.EAST, Block.box(1, 1, 1, 13, 16, 15));
        builder.put(Direction.WEST, Block.box(3, 1, 1, 15, 16, 15));
        DEFAULT_SHAPES = new EnumMap<>(builder.build());
    }
}
