package net.fabricmc.err.dispensablecauldron.mixin;

import net.fabricmc.err.dispensablecauldron.Config;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.block.AbstractCauldronBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.LavaCauldronBlock;
import net.minecraft.block.LeveledCauldronBlock;
import net.minecraft.block.PowderSnowCauldronBlock;
import net.minecraft.block.dispenser.ItemDispenserBehavior;
import net.minecraft.block.entity.DispenserBlockEntity;
import net.minecraft.item.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPointer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.event.GameEvent;

@Mixin(ItemDispenserBehavior.class)
public abstract class ItemDispenserBehaviorMixin
{
	public void updateCauldron(ServerWorld world, BlockPos position, BlockState newBlockState)
	{
		world.setBlockState(position, newBlockState);
		world.emitGameEvent(GameEvent.BLOCK_CHANGE, position, GameEvent.Emitter.of(newBlockState));
	}

	public ItemStack drainCauldron(AbstractCauldronBlock cauldron, ServerWorld world, BlockPointer pointer, BlockPos position, ItemStack stack)
	{
		ItemStack newItemstack = new ItemStack(Items.WATER_BUCKET);
		if(cauldron instanceof PowderSnowCauldronBlock) newItemstack = new ItemStack(Items.POWDER_SNOW_BUCKET);
		else if(cauldron instanceof LavaCauldronBlock) newItemstack = new ItemStack(Items.LAVA_BUCKET);
		Item newItem = newItemstack.getItem();

		updateCauldron(world, position, Blocks.CAULDRON.getDefaultState());

		stack.decrement(1);
		if (stack.isEmpty()) return new ItemStack(newItem);

		if (((DispenserBlockEntity)pointer.getBlockEntity()).addToFirstFreeSlot(new ItemStack(newItem)) < 0)
		{
			ItemDispenserBehavior fallbackBehavior = new ItemDispenserBehavior();
			fallbackBehavior.dispense(pointer, new ItemStack(newItem));
		}

		return stack;
	}

	public ItemStack fillCauldron(AbstractCauldronBlock cauldron, Item bucket, ServerWorld world, BlockPointer pointer, BlockPos position, ItemStack stack)
	{
		BlockState blockState = Blocks.WATER_CAULDRON.getDefaultState().with(LeveledCauldronBlock.LEVEL, 3);
		if (bucket == Items.POWDER_SNOW_BUCKET) blockState = Blocks.POWDER_SNOW_CAULDRON.getDefaultState().with(LeveledCauldronBlock.LEVEL, 3);
		else if (bucket == Items.LAVA_BUCKET) blockState = Blocks.LAVA_CAULDRON.getDefaultState();

		updateCauldron(world, position, blockState);

		return new ItemStack(Items.BUCKET);
	}

	@Inject(method = "dispenseSilently", at = @At("HEAD"), cancellable = true)
	protected void dispenseSilently(BlockPointer pointer, ItemStack stack, CallbackInfoReturnable<ItemStack> cir)
	{
		if (Config.enable())
		{
			BlockPos position;
			ServerWorld world = pointer.getWorld();
			BlockState blockState = world.getBlockState(position = pointer.getPos().offset(pointer.getBlockState().get(DispenserBlock.FACING)));
			Block block = blockState.getBlock();
			Item usedItem = stack.getItem();

			if (block instanceof AbstractCauldronBlock && (usedItem instanceof BucketItem || usedItem instanceof PowderSnowBucketItem))
			{
				AbstractCauldronBlock cauldron = ((AbstractCauldronBlock)((Object)block));
				Boolean filledBucket = usedItem == Items.POWDER_SNOW_BUCKET || usedItem == Items.LAVA_BUCKET || usedItem == Items.WATER_BUCKET;

				if(filledBucket && cauldron == Blocks.CAULDRON)
				{
					cir.setReturnValue(fillCauldron(cauldron, usedItem, world, pointer, position, stack));
				}
				else if (cauldron.isFull(blockState) && usedItem == Items.BUCKET)
				{
					cir.setReturnValue(drainCauldron(cauldron, world, pointer, position, stack));
				}
			}
		}
	}
}
