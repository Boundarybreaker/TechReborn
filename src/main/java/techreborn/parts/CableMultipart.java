package techreborn.parts;

import mcmultipart.MCMultiPartMod;
import mcmultipart.microblock.IMicroblock;
import mcmultipart.multipart.*;
import mcmultipart.raytrace.PartMOP;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.world.World;
import net.minecraftforge.common.property.ExtendedBlockState;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;
import net.minecraftforge.common.property.Properties;
import reborncore.api.power.IEnergyInterfaceTile;
import reborncore.common.misc.Functions;
import reborncore.common.misc.vecmath.Vecs3dCube;
import techreborn.config.ConfigTechReborn;
import techreborn.parts.walia.IPartWaliaProvider;
import techreborn.power.TRPowerNet;
import techreborn.utils.damageSources.ElectrialShockSource;

import java.util.*;

/**
 * Created by modmuss50 on 02/03/2016.
 */
public abstract class CableMultipart extends Multipart implements IOccludingPart, ISlottedPart, ITickable, ICableType, ICollidableMultipart, IPartWaliaProvider {

    public Vecs3dCube[] boundingBoxes = new Vecs3dCube[14];
    public float center = 0.6F;
    public float offset = 0.10F;
    public Map<EnumFacing, BlockPos> connectedSides;
    public int ticks = 0;
    public ItemStack stack;

    public static final IUnlistedProperty<Boolean> UP = Properties.toUnlisted(PropertyBool.create("up"));
    public static final IUnlistedProperty<Boolean> DOWN = Properties.toUnlisted(PropertyBool.create("down"));
    public static final IUnlistedProperty<Boolean> NORTH = Properties.toUnlisted(PropertyBool.create("north"));
    public static final IUnlistedProperty<Boolean> EAST = Properties.toUnlisted(PropertyBool.create("east"));
    public static final IUnlistedProperty<Boolean> SOUTH = Properties.toUnlisted(PropertyBool.create("south"));
    public static final IUnlistedProperty<Boolean> WEST = Properties.toUnlisted(PropertyBool.create("west"));
    public static final IProperty<EnumCableType> TYPE = PropertyEnum.create("type", EnumCableType.class);

    public CableMultipart() {
        connectedSides = new HashMap<>();
        refreshBounding();
    }

    public void refreshBounding() {
        float centerFirst = center - offset;
        double w = (getCableType().cableThickness / 16) - 0.5;
        boundingBoxes[6] = new Vecs3dCube(centerFirst - w - 0.03, centerFirst
                - w - 0.08, centerFirst - w - 0.03, centerFirst + w + 0.08,
                centerFirst + w + 0.04, centerFirst + w + 0.08);

        boundingBoxes[6] = new Vecs3dCube(centerFirst - w, centerFirst - w,
                centerFirst - w, centerFirst + w, centerFirst + w, centerFirst
                + w);

        int i = 0;
        for (EnumFacing dir : EnumFacing.VALUES) {
            double xMin1 = (dir.getFrontOffsetX() < 0 ? 0.0
                    : (dir.getFrontOffsetX() == 0 ? centerFirst - w : centerFirst + w));
            double xMax1 = (dir.getFrontOffsetX() > 0 ? 1.0
                    : (dir.getFrontOffsetX() == 0 ? centerFirst + w : centerFirst - w));

            double yMin1 = (dir.getFrontOffsetY() < 0 ? 0.0
                    : (dir.getFrontOffsetY() == 0 ? centerFirst - w : centerFirst + w));
            double yMax1 = (dir.getFrontOffsetY() > 0 ? 1.0
                    : (dir.getFrontOffsetY() == 0 ? centerFirst + w : centerFirst - w));

            double zMin1 = (dir.getFrontOffsetZ() < 0 ? 0.0
                    : (dir.getFrontOffsetZ() == 0 ? centerFirst - w : centerFirst + w));
            double zMax1 = (dir.getFrontOffsetZ() > 0 ? 1.0
                    : (dir.getFrontOffsetZ() == 0 ? centerFirst + w : centerFirst - w));

            boundingBoxes[i] = new Vecs3dCube(xMin1, yMin1, zMin1, xMax1,
                    yMax1, zMax1);
            i++;
        }
    }

    @Override
    public void addCollisionBoxes(AxisAlignedBB mask, List<AxisAlignedBB> list, Entity collidingEntity) {
        super.addCollisionBoxes(mask, list, collidingEntity);
        for (EnumFacing dir : EnumFacing.VALUES) {
            if (connectedSides.containsKey(dir) && mask.intersectsWith(boundingBoxes[Functions.getIntDirFromDirection(dir)].toAABB()))
                list.add(boundingBoxes[Functions.getIntDirFromDirection(dir)].toAABB());
        }
        if (mask.intersectsWith(boundingBoxes[6].toAABB())) {
            list.add(boundingBoxes[6].toAABB());
        }
    }

    @Override
    public void addSelectionBoxes(List<AxisAlignedBB> list) {
        super.addSelectionBoxes(list);
        for (EnumFacing dir : EnumFacing.VALUES) {
            if (connectedSides.containsKey(dir))
                list.add(boundingBoxes[Functions.getIntDirFromDirection(dir)].toAABB());
        }
        list.add(boundingBoxes[6].toAABB());
    }

    @Override
    public void onRemoved() {
        super.onRemoved();
        removeFromNetwork();
    }

    @Override
    public void onUnloaded() {
        super.onUnloaded();
        removeFromNetwork();
    }

    @Override
    public void addOcclusionBoxes(List<AxisAlignedBB> list) {
        for (EnumFacing dir : EnumFacing.VALUES) {
            if (connectedSides.containsKey(dir))
                list.add(boundingBoxes[Functions.getIntDirFromDirection(dir)].toAABB());
        }
        list.add(boundingBoxes[6].toAABB());
    }

    @Override
    public void onNeighborBlockChange(Block block) {
        super.onNeighborBlockChange(block);
        nearByChange();
        findAndJoinNetwork(getWorld(), getPos());
    }

    public void nearByChange() {
        checkConnectedSides();
        for (EnumFacing direction : EnumFacing.VALUES) {
            BlockPos blockPos = getPos().offset(direction);
            getWorld().markBlockForUpdate(blockPos);
            CableMultipart part = getPartFromWorld(getWorld(), blockPos, direction);
            if (part != null) {
                part.checkConnectedSides();
            }
        }
    }

    public CableMultipart getPartFromWorld(World world, BlockPos pos, EnumFacing side) {
        if(world == null || pos == null){
            return null;
        }
        IMultipartContainer container = MultipartHelper.getPartContainer(world, pos);
        if (side != null && container != null) {
            ISlottedPart slottedPart = container.getPartInSlot(PartSlot.getFaceSlot(side));
            if (slottedPart instanceof IMicroblock.IFaceMicroblock && !((IMicroblock.IFaceMicroblock) slottedPart).isFaceHollow()) {
                return null;
            }
        }

        if (container == null) {
            return null;
        }
        ISlottedPart part = container.getPartInSlot(PartSlot.CENTER);
        if (part instanceof CableMultipart) {
            return (CableMultipart) part;
        }
        return null;
    }

    @Override
    public void onAdded() {
        nearByChange();
    }

    public boolean shouldConnectTo(EnumFacing dir) {
        if (dir != null) {
            if (internalShouldConnectTo(dir)) {
                CableMultipart cableMultipart = getPartFromWorld(getWorld(), getPos().offset(dir), dir);
                if (cableMultipart != null && cableMultipart.internalShouldConnectTo(dir.getOpposite())) {
                    return true;
                }
            } else {
                TileEntity tile = getNeighbourTile(dir);

                if (tile instanceof IEnergyInterfaceTile) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean internalShouldConnectTo(EnumFacing dir) {
        ISlottedPart part = getContainer().getPartInSlot(PartSlot.getFaceSlot(dir));
        if (part instanceof IMicroblock.IFaceMicroblock) {
            if (!((IMicroblock.IFaceMicroblock) part).isFaceHollow()) {
                return false;
            }
        }

        if (!OcclusionHelper.occlusionTest(getContainer().getParts(), this, boundingBoxes[Functions.getIntDirFromDirection(dir)].toAABB())) {
            return false;
        }

        CableMultipart cableMultipart = getPartFromWorld(getWorld(), getPos().offset(dir), dir.getOpposite());

        if (cableMultipart != null && cableMultipart.getCableType() == getCableType()) {
            return true;
        }
        return false;
    }

    public TileEntity getNeighbourTile(EnumFacing side) {
        return side != null ? getWorld().getTileEntity(getPos().offset(side)) : null;
    }

    public void checkConnectedSides() {
        refreshBounding();
        connectedSides = new HashMap<>();
        for (EnumFacing dir : EnumFacing.values()) {
            int d = Functions.getIntDirFromDirection(dir);
            if (getWorld() == null) {
                return;
            }
            TileEntity te = getNeighbourTile(dir);
            if (shouldConnectTo(dir)) {
                connectedSides.put(dir, te.getPos());
            }
        }
    }

    @Override
    public EnumSet<PartSlot> getSlotMask() {
        return EnumSet.of(PartSlot.CENTER);
    }

    @Override
    public void update() {
        if (getWorld() != null) {
            if (getWorld().getTotalWorldTime() % 80 == 0) {
                checkConnectedSides();
            }
        }
        if (network == null) {
            this.findAndJoinNetwork(getWorld(), getPos());
        }
    }

    @Override
    public IBlockState getExtendedState(IBlockState state) {
        IExtendedBlockState extendedBlockState = (IExtendedBlockState) state;
        return extendedBlockState
                .withProperty(DOWN, shouldConnectTo(EnumFacing.DOWN))
                .withProperty(UP, shouldConnectTo(EnumFacing.UP))
                .withProperty(NORTH, shouldConnectTo(EnumFacing.NORTH))
                .withProperty(SOUTH, shouldConnectTo(EnumFacing.SOUTH))
                .withProperty(WEST, shouldConnectTo(EnumFacing.WEST))
                .withProperty(EAST, shouldConnectTo(EnumFacing.EAST))
                .withProperty(TYPE, getCableType());
    }

    @Override
    public BlockState createBlockState() {
        return new ExtendedBlockState(MCMultiPartMod.multipart,
                new IProperty[]{
                        TYPE
                },
                new IUnlistedProperty[]{
                        DOWN,
                        UP,
                        NORTH,
                        SOUTH,
                        WEST,
                        EAST});
    }


    @Override
    public String getModelPath() {
        return "techreborn:cable";
    }

    @Override
    public float getHardness(PartMOP hit) {
        return 0.5F;
    }

    public Material getMaterial() {
        return Material.cloth;
    }

    @Override
    public List<ItemStack> getDrops() {
        List<ItemStack> list = new ArrayList<>();
        list.add(new ItemStack(TechRebornParts.cables, 1, getCableType().ordinal()));
        return list;
    }


    @Override
    public void onEntityCollided(Entity entity) {
        if (getCableType().canKill && entity instanceof EntityLivingBase) {
        	if(ConfigTechReborn.UninsulatedElectocutionDamage){
                entity.attackEntityFrom(new ElectrialShockSource(), 1F);
        	}
        	if(ConfigTechReborn.UninsulatedElectocutionSound){
                getWorld().playSoundAtEntity(entity, "techreborn:cable_shock", 0.6F, 1F);
        	}
        	if(ConfigTechReborn.UninsulatedElectocutionParticle){
                getWorld().spawnParticle(EnumParticleTypes.CRIT, entity.posX, entity.posY, entity.posZ, 0, 0, 0);
        	}
        }
    }

    @Override
    public void onEntityStanding(Entity entity) {

    }

    @Override
    public ItemStack getPickBlock(EntityPlayer player, PartMOP hit) {
        return new ItemStack(TechRebornParts.cables, 1, getCableType().ordinal());
    }

    private TRPowerNet network;

    public final void findAndJoinNetwork(World world, BlockPos pos) {
        for (EnumFacing dir : EnumFacing.VALUES) {
            CableMultipart cableMultipart = getPartFromWorld(getWorld(), getPos().offset(dir), dir);
            if (cableMultipart != null && cableMultipart.getCableType() == getCableType()) {
                TRPowerNet net = cableMultipart.getNetwork();
                if (net != null) {
                    network = net;
                    network.addElement(this);
                    break;
                }
            }
        }
        if(network == null){
            network = new TRPowerNet(getCableType());
            network.addElement(this);
        }
        network.endpoints.clear();
        for (EnumFacing dir : EnumFacing.VALUES) {
            TileEntity te = getNeighbourTile(dir);
            if (te != null && te instanceof IEnergyInterfaceTile) {
                network.addConnection((IEnergyInterfaceTile) te, dir.getOpposite());
            }
        }
    }

    public final TRPowerNet getNetwork() {
        return network;
    }

    public final void setNetwork(TRPowerNet n) {
        if (n == null) {
        } else {
            network = n;
            network.addElement(this);
        }
    }

    public final void removeFromNetwork() {
        if (network == null) {
        } else
            network.removeElement(this);
    }

    public final void rebuildNetwork() {
        this.removeFromNetwork();
        this.resetNetwork();
        this.findAndJoinNetwork(getWorld(), getPos());
    }

    public final void resetNetwork() {
        if(network != null){
            network.removeElement(this);
        }

        network = null;
    }

    @Override
    public void addInfo(List<String> info) {
        info.add(EnumChatFormatting.GREEN + "EU Transfer: " + EnumChatFormatting.LIGHT_PURPLE + getCableType().transferRate);
        if (getCableType().canKill) {
            info.add(EnumChatFormatting.RED + "Damages entity's!");
        }
    }

    @Override
    public boolean canRenderInLayer(EnumWorldBlockLayer layer) {
        return layer == EnumWorldBlockLayer.CUTOUT;
    }
}
