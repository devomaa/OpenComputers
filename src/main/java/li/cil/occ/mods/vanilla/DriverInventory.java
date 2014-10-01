package li.cil.occ.mods.vanilla;

import li.cil.oc.api.network.Arguments;
import li.cil.oc.api.network.Callback;
import li.cil.oc.api.network.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverTileEntity;
import li.cil.occ.OpenComponents;
import li.cil.occ.mods.ManagedTileEntityEnvironment;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public final class DriverInventory extends DriverTileEntity {
    @Override
    public Class<?> getTileEntityClass() {
        return IInventory.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final int x, final int y, final int z) {
        return new Environment((IInventory) world.getTileEntity(x, y, z));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<IInventory> {
        public Environment(final IInventory tileEntity) {
            super(tileEntity, "inventory");
        }

        @Callback(doc = "function():string -- Get the name of this inventory.")
        public Object[] getInventoryName(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getInventoryName()};
        }

        @Callback(doc = "function():number -- Get the number of slots in this inventory.")
        public Object[] getInventorySize(final Context context, final Arguments args) {
            return new Object[]{tileEntity.getSizeInventory()};
        }

        @Callback(doc = "function(slot:number):number -- Get the stack size of the item stack in the specified slot.")
        public Object[] getSlotStackSize(final Context context, final Arguments args) {
            final int slot = checkSlot(args, 0);
            final ItemStack stack = tileEntity.getStackInSlot(slot);
            if (stack != null) {
                return new Object[]{stack.stackSize};
            } else {
                return new Object[]{0};
            }
        }

        @Callback(doc = "function(slot:number):number -- Get the maximum stack size of the item stack in the specified slot.")
        public Object[] getSlotMaxStackSize(final Context context, final Arguments args) {
            final int slot = checkSlot(args, 0);
            final ItemStack stack = tileEntity.getStackInSlot(slot);
            if (stack != null) {
                return new Object[]{Math.min(tileEntity.getInventoryStackLimit(), stack.getMaxStackSize())};
            } else {
                return new Object[]{tileEntity.getInventoryStackLimit()};
            }
        }

        @Callback(doc = "function(slotA:number, slotB:number):boolean -- Compare the two item stacks in the specified slots for equality.")
        public Object[] compareStacks(final Context context, final Arguments args) {
            final int slotA = checkSlot(args, 0);
            final int slotB = checkSlot(args, 1);
            if (slotA == slotB) {
                return new Object[]{true};
            }
            final ItemStack stackA = tileEntity.getStackInSlot(slotA);
            final ItemStack stackB = tileEntity.getStackInSlot(slotB);
            if (stackA == null && stackB == null) {
                return new Object[]{true};
            } else if (stackA != null && stackB != null) {
                return new Object[]{itemEquals(stackA, stackB)};
            } else {
                return new Object[]{false};
            }
        }

        @Callback(doc = "function(slotA:number, slotB:number[, count:number=math.huge]):boolean -- Move up to the specified number of items from the first specified slot to the second.")
        public Object[] transferStack(final Context context, final Arguments args) {
            final int slotA = checkSlot(args, 0);
            final int slotB = checkSlot(args, 1);
            final int count = Math.max(0, Math.min(args.count() > 2 && args.checkAny(2) != null ? args.checkInteger(2) : 64, tileEntity.getInventoryStackLimit()));
            if (slotA == slotB || count == 0) {
                return new Object[]{true};
            }
            final ItemStack stackA = tileEntity.getStackInSlot(slotA);
            final ItemStack stackB = tileEntity.getStackInSlot(slotB);
            if (stackA == null) {
                // Empty.
                return new Object[]{false};
            } else if (stackB == null) {
                // Move.
                tileEntity.setInventorySlotContents(slotB, tileEntity.decrStackSize(slotA, count));
                return new Object[]{true};
            } else if (itemEquals(stackA, stackB)) {
                // Pile.
                final int space = Math.min(tileEntity.getInventoryStackLimit(), stackB.getMaxStackSize()) - stackB.stackSize;
                final int amount = Math.min(count, Math.min(space, stackA.stackSize));
                if (amount > 0) {
                    // Some.
                    stackA.stackSize -= amount;
                    stackB.stackSize += amount;
                    if (stackA.stackSize == 0) {
                        tileEntity.setInventorySlotContents(slotA, null);
                    }
                    tileEntity.markDirty();
                    return new Object[]{true};
                }
            } else if (count >= stackA.stackSize) {
                // Swap.
                tileEntity.setInventorySlotContents(slotB, stackA);
                tileEntity.setInventorySlotContents(slotA, stackB);
                return new Object[]{true};
            }
            // Fail.
            return new Object[]{false};
        }

        @Callback(doc = "function(slot:number):table -- Get a description of the item stack in the specified slot.")
        public Object[] getStackInSlot(final Context context, final Arguments args) {
            if (OpenComponents.allowItemStackInspection) {
                return new Object[]{tileEntity.getStackInSlot(checkSlot(args, 0))};
            } else {
                return new Object[]{null, "not enabled in config"};
            }
        }

        @Callback(doc = "function():table -- Get a list of descriptions for all item stacks in this inventory.")
        public Object[] getAllStacks(final Context context, final Arguments args) {
            if (OpenComponents.allowItemStackInspection) {
                ItemStack[] allStacks = new ItemStack[tileEntity.getSizeInventory()];
                for (int i = 0; i < tileEntity.getSizeInventory(); i++) {
                    allStacks[i] = tileEntity.getStackInSlot(i);
                }
                return new Object[]{allStacks};
            } else {
                return new Object[]{null, "not enabled in config"};
            }
        }

        private int checkSlot(final Arguments args, final int number) {
            final int slot = args.checkInteger(number) - 1;
            if (slot < 0 || slot >= tileEntity.getSizeInventory()) {
                throw new IllegalArgumentException("slot index out of bounds");
            }
            return slot;
        }

        private boolean itemEquals(final ItemStack stackA, final ItemStack stackB) {
            return stackA.getItem().equals(stackB.getItem()) && !stackA.getHasSubtypes() || stackA.getItemDamage() == stackB.getItemDamage();
        }
    }
}
