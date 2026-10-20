/*
 * Copyright (C) 2019 BARBOTIN Nicolas
 */

package net.montoyo.wd.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.montoyo.mcef.api.IBrowser;
import net.montoyo.wd.WDConfig;
import net.montoyo.wd.WDContent;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.block.ScreenBlock;
import net.montoyo.wd.core.DefaultUpgrade;
import net.montoyo.wd.core.IUpgrade;
import net.montoyo.wd.core.JSServerRequest;
import net.montoyo.wd.core.ScreenRights;
import net.montoyo.wd.net.C2SRequestTEData;
import net.montoyo.wd.net.S2CCloseGui;
import net.montoyo.wd.net.S2CJSResponse;
import net.montoyo.wd.net.S2CScreenUpdate;
import net.montoyo.wd.platform.Platform;
import net.montoyo.wd.utilities.BlockSide;
import net.montoyo.wd.utilities.Log;
import net.montoyo.wd.utilities.NameUUIDPair;
import net.montoyo.wd.utilities.NibbleArray;
import net.montoyo.wd.utilities.Rotation;
import net.montoyo.wd.utilities.ScreenIterator;
import net.montoyo.wd.utilities.TypeData;
import net.montoyo.wd.utilities.Util;
import net.montoyo.wd.utilities.Vector2i;
import net.montoyo.wd.utilities.Vector3f;
import net.montoyo.wd.utilities.Vector3i;
import net.montoyo.wd.utilities.VideoType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * The block entity that owns every screen face of one multiblock.
 * <p>
 * 1.12.2 called this {@code TileEntityScreen}. The behaviour is carried over; only the 1.20.1 API
 * surface changed ({@code BlockEntity}, {@code CompoundTag}, {@code Level}, no
 * {@code SimpleNetworkWrapper}).
 */
public class ScreenBlockEntity extends BlockEntity {

    public static class Screen {

        public BlockSide side;
        public Vector2i size;
        public Vector2i resolution;
        public Rotation rotation = Rotation.ROT_0;
        public String url;
        public VideoType videoType;
        public NameUUIDPair owner;
        public ArrayList<NameUUIDPair> friends = new ArrayList<>();
        public int friendRights;
        public int otherRights;
        public IBrowser browser;
        public ArrayList<ItemStack> upgrades = new ArrayList<>();
        public boolean doTurnOnAnim;
        public long turnOnTime;
        public Player laserUser;
        public final Vector2i lastMousePos = new Vector2i();
        /** null on client. */
        public NibbleArray redstoneStatus;
        public boolean autoVolume = true;

        public static Screen deserialize(CompoundTag tag) {
            Screen ret = new Screen();
            ret.side = BlockSide.values()[tag.getByte("Side")];
            ret.size = new Vector2i(tag.getInt("Width"), tag.getInt("Height"));
            ret.resolution = new Vector2i(tag.getInt("ResolutionX"), tag.getInt("ResolutionY"));
            ret.rotation = Rotation.values()[tag.getByte("Rotation")];
            ret.url = tag.getString("URL");
            ret.videoType = VideoType.getTypeFromURL(ret.url);

            if (ret.resolution.x <= 0 || ret.resolution.y <= 0) {
                float psx = ((float) ret.size.x) * 16.f - 4.f;
                float psy = ((float) ret.size.y) * 16.f - 4.f;
                psx *= 8.f;
                psy *= 8.f;

                ret.resolution.x = (int) psx;
                ret.resolution.y = (int) psy;
            }

            if (tag.contains("OwnerName")) {
                String name = tag.getString("OwnerName");
                UUID uuid = tag.getUUID("OwnerUUID");
                ret.owner = new NameUUIDPair(name, uuid);
            }

            ListTag friends = tag.getList("Friends", Tag.TAG_COMPOUND);
            ret.friends = new ArrayList<>(friends.size());

            for (int i = 0; i < friends.size(); i++) {
                CompoundTag nf = friends.getCompound(i);
                ret.friends.add(new NameUUIDPair(nf.getString("Name"), nf.getUUID("UUID")));
            }

            ret.friendRights = tag.getByte("FriendRights");
            ret.otherRights = tag.getByte("OtherRights");

            ListTag upgrades = tag.getList("Upgrades", Tag.TAG_COMPOUND);
            ret.upgrades = new ArrayList<>();

            for (int i = 0; i < upgrades.size(); i++)
                ret.upgrades.add(ItemStack.of(upgrades.getCompound(i)));

            if (tag.contains("AutoVolume"))
                ret.autoVolume = tag.getBoolean("AutoVolume");

            return ret;
        }

        public CompoundTag serialize() {
            CompoundTag tag = new CompoundTag();
            tag.putByte("Side", (byte) side.ordinal());
            tag.putInt("Width", size.x);
            tag.putInt("Height", size.y);
            tag.putInt("ResolutionX", resolution.x);
            tag.putInt("ResolutionY", resolution.y);
            tag.putByte("Rotation", (byte) rotation.ordinal());
            tag.putString("URL", url == null ? "" : url);

            if (owner == null)
                Log.warning("Found TES with NO OWNER!!");
            else {
                tag.putString("OwnerName", owner.name);
                tag.putUUID("OwnerUUID", owner.uuid);
            }

            ListTag list = new ListTag();

            for (NameUUIDPair f : friends) {
                CompoundTag nf = new CompoundTag();
                nf.putString("Name", f.name);
                nf.putUUID("UUID", f.uuid);
                list.add(nf);
            }

            tag.put("Friends", list);
            tag.putByte("FriendRights", (byte) friendRights);
            tag.putByte("OtherRights", (byte) otherRights);

            list = new ListTag();

            for (ItemStack is : upgrades)
                list.add(is.save(new CompoundTag()));

            tag.put("Upgrades", list);
            tag.putBoolean("AutoVolume", autoVolume);
            return tag;
        }

        public int rightsFor(Player ply) {
            return rightsFor(ply.getUUID());
        }

        public int rightsFor(UUID uuid) {
            if (owner != null && owner.uuid.equals(uuid))
                return ScreenRights.ALL;

            return friends.stream().anyMatch(f -> f.uuid.equals(uuid)) ? friendRights : otherRights;
        }

        public void setupRedstoneStatus(Level world, BlockPos start) {
            if (world.isClientSide) {
                Log.warning("Called Screen.setupRedstoneStatus() on client.");
                return;
            }

            if (redstoneStatus != null) {
                Log.warning("Called Screen.setupRedstoneStatus() on server, but redstone status is non-null");
                return;
            }

            redstoneStatus = new NibbleArray(size.x * size.y);
            final Direction facing = Direction.values()[side.reverse().ordinal()];
            final ScreenIterator it = new ScreenIterator(start, side, size);

            while (it.hasNext()) {
                int idx = it.getIndex();
                redstoneStatus.set(idx, world.getSignal(it.next(), facing));
            }
        }

        public void clampResolution() {
            WDConfig cfg = WDConfig.get();

            if (resolution.x > cfg.maxResolutionX) {
                float newY = ((float) resolution.y) * ((float) cfg.maxResolutionX) / ((float) resolution.x);
                resolution.x = cfg.maxResolutionX;
                resolution.y = (int) newY;
            }

            if (resolution.y > cfg.maxResolutionY) {
                float newX = ((float) resolution.x) * ((float) cfg.maxResolutionY) / ((float) resolution.y);
                resolution.x = (int) newX;
                resolution.y = cfg.maxResolutionY;
            }
        }
    }

    private final ArrayList<Screen> screens = new ArrayList<>();
    private boolean loaded = true;
    public float ytVolume = Float.POSITIVE_INFINITY;

    public ScreenBlockEntity(BlockPos pos, BlockState state) {
        super(WDContent.SCREEN_BE_TYPE.get(), pos, state);
    }

    public boolean isLoaded() {
        return loaded;
    }

    public void load() {
        loaded = true;
    }

    public void unload() {
        for (Screen scr : screens) {
            if (scr.browser != null) {
                scr.browser.close();
                scr.browser = null;
            }
        }

        loaded = false;
    }

    public void forEachScreenBlocks(BlockSide side, Consumer<BlockPos> func) {
        Screen scr = getScreen(side);

        if (scr != null) {
            ScreenIterator it = new ScreenIterator(worldPosition, side, scr.size);

            while (it.hasNext())
                func.accept(it.next());
        }
    }

    // ---- persistence -----------------------------------------------------

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);

        ListTag list = tag.getList("WDScreens", Tag.TAG_COMPOUND);

        if (list.isEmpty())
            return;

        screens.clear();

        for (int i = 0; i < list.size(); i++)
            screens.add(Screen.deserialize(list.getCompound(i)));
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);

        ListTag list = new ListTag();

        for (Screen scr : screens)
            list.add(scr.serialize());

        tag.put("WDScreens", list);
    }

    // ---- networking helpers ---------------------------------------------

    private void sendAround(net.montoyo.wd.platform.WDPacket packet) {
        Platform.get().net().sendToAllAround(packet, worldPosition, 64.0);
    }

    /**
     * Sends every screen of this block entity to a single player, used when the chunk is (re)loaded on
     * the client.
     */
    public void sendFullState(ServerPlayer player) {
        for (Screen scr : screens) {
            S2CScreenUpdate p = new S2CScreenUpdate();
            p.action = S2CScreenUpdate.ADD;
            p.pos = new Vector3i(worldPosition);
            p.side = scr.side;
            p.tag = scr.serialize();
            Platform.get().net().sendToPlayer(p, player);
        }
    }

    public void requestData(ServerPlayer player) {
        if (level != null && !level.isClientSide)
            Platform.get().net().sendToPlayer(new C2SRequestTEData(new Vector3i(worldPosition)), player);
    }

    /**
     * Serializes one screen for the configuration GUI. Replaces the 1.12.2 {@code ScreenConfigData}
     * class, which carried the same fields as a packet payload.
     */
    public String describeScreen(Screen scr) {
        var obj = new com.google.gson.JsonObject();
        obj.addProperty("url", scr.url == null ? "" : scr.url);
        obj.addProperty("sizeX", scr.size.x);
        obj.addProperty("sizeY", scr.size.y);
        obj.addProperty("resX", scr.resolution.x);
        obj.addProperty("resY", scr.resolution.y);
        obj.addProperty("rotation", scr.rotation.ordinal());
        obj.addProperty("autoVolume", scr.autoVolume);
        obj.addProperty("friendRights", scr.friendRights);
        obj.addProperty("otherRights", scr.otherRights);
        obj.addProperty("owner", scr.owner == null ? "" : scr.owner.name);

        var friends = new com.google.gson.JsonArray();

        for (NameUUIDPair f : scr.friends) {
            var fo = new com.google.gson.JsonObject();
            fo.addProperty("name", f.name);
            fo.addProperty("uuid", f.uuid.toString());
            friends.add(fo);
        }

        obj.add("friends", friends);

        var upgrades = new com.google.gson.JsonArray();

        for (ItemStack is : scr.upgrades)
            upgrades.add(safeName(is));

        obj.add("upgrades", upgrades);

        return WebDisplays.GSON.toJson(obj);
    }

    // ---- screens ---------------------------------------------------------

    public Screen addScreen(BlockSide side, Vector2i size, @Nullable Vector2i resolution,
                            @Nullable Player owner, boolean sendUpdate) {
        for (Screen scr : screens) {
            if (scr.side == side)
                return scr;
        }

        Screen ret = new Screen();
        ret.side = side;
        ret.size = size;
        ret.url = WDConfig.get().homepage;
        ret.friends = new ArrayList<>();
        ret.friendRights = ScreenRights.DEFAULTS;
        ret.otherRights = ScreenRights.DEFAULTS;
        ret.upgrades = new ArrayList<>();

        if (owner != null) {
            ret.owner = new NameUUIDPair(owner.getGameProfile());

            if (side == BlockSide.TOP || side == BlockSide.BOTTOM) {
                int rot = (int) Math.floor(((double) (owner.getYRot() * 4.0f / 360.0f)) + 2.5) & 3;

                if (side == BlockSide.TOP) {
                    if (rot == 1)
                        rot = 3;
                    else if (rot == 3)
                        rot = 1;
                }

                ret.rotation = Rotation.values()[rot];
            }
        }

        if (resolution == null || resolution.x < 1 || resolution.y < 1) {
            float psx = ((float) size.x) * 16.f - 4.f;
            float psy = ((float) size.y) * 16.f - 4.f;
            psx *= 8.f;
            psy *= 8.f;

            ret.resolution = new Vector2i((int) psx, (int) psy);
        } else
            ret.resolution = resolution;

        ret.clampResolution();

        if (level != null && !level.isClientSide) {
            ret.setupRedstoneStatus(level, worldPosition);

            if (sendUpdate) {
                S2CScreenUpdate p = new S2CScreenUpdate();
                p.action = S2CScreenUpdate.ADD;
                p.pos = new Vector3i(worldPosition);
                p.side = side;
                p.tag = ret.serialize();
                sendAround(p);
            }
        }

        screens.add(ret);
        setChanged();
        return ret;
    }

    public Screen getScreen(BlockSide side) {
        for (Screen scr : screens) {
            if (scr.side == side)
                return scr;
        }

        return null;
    }

    public int screenCount() {
        return screens.size();
    }

    public Screen getScreen(int idx) {
        return screens.get(idx);
    }

    public List<Screen> getScreens() {
        return Collections.unmodifiableList(screens);
    }

    public void clear() {
        screens.clear();

        if (level != null && !level.isClientSide)
            setChanged();
    }

    /**
     * Client-side only: applies a screen snapshot coming from the server, keeping the live browser
     * instance if there is one (the server does not know about it).
     */
    public void applyClientScreen(BlockSide side, CompoundTag tag) {
        Screen fresh = Screen.deserialize(tag);

        for (int i = 0; i < screens.size(); i++) {
            if (screens.get(i).side == side) {
                fresh.browser = screens.get(i).browser;
                fresh.lastMousePos.x = screens.get(i).lastMousePos.x;
                fresh.lastMousePos.y = screens.get(i).lastMousePos.y;
                screens.set(i, fresh);
                return;
            }
        }

        screens.add(fresh);
    }

    /**
     * Client-side only: drops a screen the server told us to remove.
     */
    public void removeClientScreen(BlockSide side) {
        screens.removeIf(scr -> {
            if (scr.side != side)
                return false;

            if (scr.browser != null) {
                scr.browser.close();
                scr.browser = null;
            }

            return true;
        });
    }

    public void setScreenURL(BlockSide side, String url) {
        Screen scr = getScreen(side);

        if (scr == null) {
            Log.error("Attempt to change URL of non-existing screen on side %s", side.toString());
            return;
        }

        url = WebDisplays.applyBlacklist(url);
        scr.url = url;
        scr.videoType = VideoType.getTypeFromURL(url);

        if (level != null && level.isClientSide) {
            if (scr.browser != null)
                scr.browser.loadURL(url);
        } else {
            sendAround(S2CScreenUpdate.setUrl(new Vector3i(worldPosition), side, url));
            setChanged();
        }
    }

    public void removeScreen(BlockSide side) {
        int idx = -1;

        for (int i = 0; i < screens.size(); i++) {
            if (screens.get(i).side == side) {
                idx = i;
                break;
            }
        }

        if (idx < 0) {
            Log.error("Tried to delete non-existing screen on side %s", side.toString());
            return;
        }

        if (level != null && level.isClientSide) {
            if (screens.get(idx).browser != null) {
                screens.get(idx).browser.close();
                screens.get(idx).browser = null;
            }
        } else
            sendAround(S2CScreenUpdate.remove(new Vector3i(worldPosition), side));

        screens.remove(idx);

        if (level != null && !level.isClientSide) {
            if (screens.isEmpty()) {
                BlockState bs = level.getBlockState(worldPosition);

                if (bs.getBlock() instanceof ScreenBlock)
                    level.setBlock(worldPosition, bs.setValue(ScreenBlock.HAS_TE, false), 3);
            } else
                setChanged();
        }
    }

    public void setResolution(BlockSide side, Vector2i res) {
        if (res.x < 1 || res.y < 1) {
            Log.warning("Call to ScreenBlockEntity.setResolution(%s) with suspicious values X=%d and Y=%d",
                    side.toString(), res.x, res.y);
            return;
        }

        Screen scr = getScreen(side);

        if (scr == null) {
            Log.error("Tried to change resolution of non-existing screen on side %s", side.toString());
            return;
        }

        scr.resolution = res;
        scr.clampResolution();

        if (level != null && level.isClientSide) {
            net.montoyo.wd.client.ClientProxy.get().screenUpdateResolutionInGui(new Vector3i(worldPosition), side, res);

            if (scr.browser != null) {
                scr.browser.close();
                scr.browser = null; //Will be re-created by the renderer
            }
        } else {
            sendAround(S2CScreenUpdate.setResolution(new Vector3i(worldPosition), side, res));
            setChanged();
        }
    }

    public void clearLaserUser(BlockSide side) {
        Screen scr = getScreen(side);

        if (scr != null)
            scr.laserUser = null;
    }

    private static Player getLaserUser(Screen scr) {
        if (scr.laserUser != null) {
            if (scr.laserUser.isDeadOrDying()
                    || scr.laserUser.getMainHandItem().getItem() != WDContent.LASER_POINTER.get())
                scr.laserUser = null;
        }

        return scr.laserUser;
    }

    private static void checkLaserUserRights(Screen scr) {
        if (scr.laserUser != null && (scr.rightsFor(scr.laserUser) & ScreenRights.CLICK) == 0)
            scr.laserUser = null;
    }

    public void click(BlockSide side, Vector2i vec) {
        Screen scr = getScreen(side);

        if (scr == null) {
            Log.error("Attempt click non-existing screen of side %s", side.toString());
            return;
        }

        if (level != null && level.isClientSide)
            Log.warning("ScreenBlockEntity.click() from client side is useless...");
        else if (getLaserUser(scr) == null)
            sendAround(S2CScreenUpdate.click(new Vector3i(worldPosition), side, S2CScreenUpdate.MOUSE_CLICK, vec));
    }

    public void handleMouseEvent(BlockSide side, int event, @Nullable Vector2i vec) {
        Screen scr = getScreen(side);

        if (scr == null) {
            Log.error("Attempt inject mouse events on non-existing screen of side %s", side.toString());
            return;
        }

        if (scr.browser == null)
            return;

        if (event == S2CScreenUpdate.MOUSE_CLICK) {
            scr.browser.injectMouseMove(vec.x, vec.y, 0, false);
            scr.browser.injectMouseButton(vec.x, vec.y, 0, 1, true, 1);
            scr.browser.injectMouseButton(vec.x, vec.y, 0, 1, false, 1);
        } else if (event == S2CScreenUpdate.MOUSE_DOWN) {
            scr.browser.injectMouseMove(vec.x, vec.y, 0, false);
            scr.browser.injectMouseButton(vec.x, vec.y, 0, 1, true, 1);
        } else if (event == S2CScreenUpdate.MOUSE_MOVE)
            scr.browser.injectMouseMove(vec.x, vec.y, 0, false);
        else if (event == S2CScreenUpdate.MOUSE_UP)
            scr.browser.injectMouseButton(scr.lastMousePos.x, scr.lastMousePos.y, 0, 1, false, 1);

        if (vec != null) {
            scr.lastMousePos.x = vec.x;
            scr.lastMousePos.y = vec.y;
        }
    }

    public void updateJSRedstone(BlockSide side, Vector2i vec, int redstoneLevel) {
        Screen scr = getScreen(side);

        if (scr == null) {
            Log.error("Called updateJSRedstone on non-existing side %s", side.toString());
            return;
        }

        if (level != null && level.isClientSide) {
            if (scr.browser != null)
                scr.browser.runJS("if(typeof webdisplaysRedstoneCallback == \"function\") webdisplaysRedstoneCallback("
                        + vec.x + ", " + vec.y + ", " + redstoneLevel + ");", "");
        } else {
            boolean sendMsg = false;

            if (scr.redstoneStatus == null) {
                scr.setupRedstoneStatus(level, worldPosition);
                sendMsg = true;
            } else {
                int idx = vec.y * scr.size.x + vec.x;

                if (scr.redstoneStatus.get(idx) != redstoneLevel) {
                    scr.redstoneStatus.set(idx, redstoneLevel);
                    sendMsg = true;
                }
            }

            if (sendMsg)
                sendAround(S2CScreenUpdate.jsRedstone(new Vector3i(worldPosition), side, vec, redstoneLevel));
        }
    }

    public void handleJSRequest(ServerPlayer src, BlockSide side, int reqId, JSServerRequest req, Object[] data) {
        if (level != null && level.isClientSide) {
            Log.error("Called handleJSRequest client-side");
            return;
        }

        Screen scr = getScreen(side);

        if (scr == null) {
            Log.error("Called handleJSRequest on non-existing side %s", side.toString());
            Platform.get().net().sendToPlayer(new S2CJSResponse(reqId, 403, "Invalid side"), src);
            return;
        }

        if (scr.owner == null || !scr.owner.uuid.equals(src.getUUID())) {
            Log.warning("Player %s (UUID %s) tries to use the redstone output API on a screen he doesn't own!",
                    src.getName().getString(), src.getUUID());
            Platform.get().net().sendToPlayer(new S2CJSResponse(reqId, 403, "Only the owner can do that"), src);
            return;
        }

        if (scr.upgrades.stream().noneMatch(DefaultUpgrade.REDSTONE_OUTPUT::matches)) {
            Platform.get().net().sendToPlayer(new S2CJSResponse(reqId, 403, "Missing upgrade"), src);
            return;
        }

        if (req == JSServerRequest.CLEAR_REDSTONE) {
            final BlockPos.MutableBlockPos mbp = new BlockPos.MutableBlockPos();
            final Vector3i vec1 = new Vector3i(worldPosition);
            final Vector3i vec2 = new Vector3i();

            for (int y = 0; y < scr.size.y; y++) {
                vec2.set(vec1);

                for (int x = 0; x < scr.size.x; x++) {
                    vec2.toBlock(mbp);

                    BlockState bs = level.getBlockState(mbp);

                    if (bs.getBlock() instanceof ScreenBlock && bs.getValue(ScreenBlock.EMITTING))
                        level.setBlock(mbp, bs.setValue(ScreenBlock.EMITTING, false), 3);

                    vec2.add(side.right.x, side.right.y, side.right.z);
                }

                vec1.add(side.up.x, side.up.y, side.up.z);
            }

            Platform.get().net().sendToPlayer(new S2CJSResponse(reqId, 200, new byte[0]), src);
        } else if (req == JSServerRequest.SET_REDSTONE_AT) {
            int x = (Integer) data[0];
            int y = (Integer) data[1];
            boolean state = (Boolean) data[2];

            if (x < 0 || x >= scr.size.x || y < 0 || y >= scr.size.y) {
                Platform.get().net().sendToPlayer(new S2CJSResponse(reqId, 403, "Out of range"), src);
            } else {
                BlockPos bp = (new Vector3i(worldPosition)).addMul(side.right, x).addMul(side.up, y).toBlock();
                BlockState bs = level.getBlockState(bp);

                if (bs.getBlock() instanceof ScreenBlock && bs.getValue(ScreenBlock.EMITTING) != state)
                    level.setBlock(bp, bs.setValue(ScreenBlock.EMITTING, state), 3);

                Platform.get().net().sendToPlayer(new S2CJSResponse(reqId, 200, new byte[0]), src);
            }
        } else
            Platform.get().net().sendToPlayer(new S2CJSResponse(reqId, 400, "Invalid request"), src);
    }

    @Override
    public void setLevel(@NotNull Level level) {
        super.setLevel(level);

        if (level.isClientSide) {
            Platform.get().net().sendToServer(new C2SRequestTEData(new Vector3i(worldPosition)));
            net.montoyo.wd.client.ClientProxy.get().trackScreen(this, true);
        }
    }

    @Override
    public void setRemoved() {
        if (level != null && level.isClientSide) {
            net.montoyo.wd.client.ClientProxy.get().trackScreen(this, false);

            for (Screen scr : screens) {
                if (scr.browser != null) {
                    scr.browser.close();
                    scr.browser = null;
                }
            }
        }

        super.setRemoved();
    }

    public void updateTrackDistance(double d, float masterVolume) {
        WDConfig cfg = WDConfig.get();
        boolean needsComputation = true;
        int intPart = 0;
        int fracPart = 0;

        for (Screen scr : screens) {
            if (scr.autoVolume && scr.videoType != null && scr.browser != null && !scr.browser.isPageLoading()) {
                if (needsComputation) {
                    float dist = (float) Math.sqrt(d);
                    float vol;

                    if (dist <= cfg.dist100)
                        vol = masterVolume * (float) cfg.ytVolume;
                    else if (dist >= cfg.dist0)
                        vol = 0.0f;
                    else
                        vol = (1.0f - (dist - (float) cfg.dist100) / ((float) cfg.dist0 - (float) cfg.dist100))
                                * masterVolume * (float) cfg.ytVolume;

                    if (Math.abs(ytVolume - vol) < 0.5f)
                        return;

                    ytVolume = vol;
                    intPart = (int) vol;
                    fracPart = ((int) (vol * 100.0f)) - intPart * 100;
                    needsComputation = false;
                }

                scr.browser.runJS(scr.videoType.getVolumeJSQuery(intPart, fracPart), "");
            }
        }
    }

    public void updateClientSideURL(IBrowser target, String url) {
        for (Screen scr : screens) {
            if (scr.browser == target) {
                boolean blacklisted = WebDisplays.isSiteBlacklisted(url);
                scr.url = blacklisted ? WebDisplays.BLACKLIST_URL : url;
                scr.videoType = VideoType.getTypeFromURL(scr.url);
                ytVolume = Float.POSITIVE_INFINITY;

                if (blacklisted)
                    scr.browser.loadURL(WebDisplays.BLACKLIST_URL);

                break;
            }
        }
    }

    public void addFriend(ServerPlayer ply, BlockSide side, NameUUIDPair pair) {
        if (level == null || level.isClientSide)
            return;

        Screen scr = getScreen(side);

        if (scr == null) {
            Log.error("Tried to add friend to invalid screen side %s", side.toString());
            return;
        }

        if (!scr.friends.contains(pair)) {
            scr.friends.add(pair);
            sendScreenConfig(scr);
            setChanged();
        }
    }

    public void removeFriend(ServerPlayer ply, BlockSide side, NameUUIDPair pair) {
        if (level == null || level.isClientSide)
            return;

        Screen scr = getScreen(side);

        if (scr == null) {
            Log.error("Tried to remove friend from invalid screen side %s", side.toString());
            return;
        }

        if (scr.friends.remove(pair)) {
            checkLaserUserRights(scr);
            sendScreenConfig(scr);
            setChanged();
        }
    }

    public void setRights(ServerPlayer ply, BlockSide side, int fr, int or) {
        if (level == null || level.isClientSide)
            return;

        Screen scr = getScreen(side);

        if (scr == null) {
            Log.error("Tried to change rights of invalid screen on side %s", side.toString());
            return;
        }

        scr.friendRights = fr;
        scr.otherRights = or;

        checkLaserUserRights(scr);
        sendScreenConfig(scr);
        setChanged();
    }

    private void sendScreenConfig(Screen scr) {
        S2CScreenUpdate p = new S2CScreenUpdate();
        p.action = S2CScreenUpdate.ADD;
        p.pos = new Vector3i(worldPosition);
        p.side = scr.side;
        p.tag = scr.serialize();
        sendAround(p);
    }

    public void type(BlockSide side, String text, BlockPos soundPos) {
        Screen scr = getScreen(side);

        if (scr == null) {
            Log.error("Tried to type on invalid screen on side %s", side.toString());
            return;
        }

        if (level != null && level.isClientSide) {
            if (scr.browser == null)
                return;

            try {
                if (text.startsWith("t")) {
                    for (int i = 1; i < text.length(); i++) {
                        char chr = text.charAt(i);

                        if (chr == 1)
                            break;

                        scr.browser.injectKeyTyped(chr, 0);
                    }
                } else {
                    TypeData[] data = WebDisplays.GSON.fromJson(text, TypeData[].class);

                    for (TypeData ev : data) {
                        switch (ev.getAction()) {
                            case PRESS -> scr.browser.injectKeyPressedByKeyCode(ev.getKeyCode(), ev.getKeyChar(), 0);
                            case RELEASE -> scr.browser.injectKeyReleasedByKeyCode(ev.getKeyCode(), ev.getKeyChar(), 0);
                            case TYPE -> scr.browser.injectKeyTyped(ev.getKeyChar(), 0);
                        }
                    }
                }
            } catch (Throwable t) {
                Log.warningEx("Suspicious keyboard type packet received...", t);
            }
        } else {
            sendAround(S2CScreenUpdate.type(new Vector3i(worldPosition), side, text));

            if (soundPos != null)
                playSoundAt(WDContent.SOUND_TYPING.get(), soundPos, 0.25f, 1.f);
        }
    }

    private void playSoundAt(SoundEvent snd, BlockPos at, float vol, float pitch) {
        if (level == null || snd == null)
            return;

        level.playSound(null, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, snd, SoundSource.BLOCKS, vol, pitch);
    }

    public void updateUpgrades(BlockSide side, ItemStack[] upgrades) {
        if (level == null || !level.isClientSide) {
            Log.error("Tried to call ScreenBlockEntity.updateUpgrades() from server side...");
            return;
        }

        Screen scr = getScreen(side);

        if (scr == null) {
            Log.error("Tried to update upgrades on invalid screen on side %s", side.toString());
            return;
        }

        scr.upgrades.clear();
        Collections.addAll(scr.upgrades, upgrades);

        if (scr.browser != null)
            scr.browser.runJS("if(typeof webdisplaysUpgradesChanged == \"function\") webdisplaysUpgradesChanged();", "");
    }

    private static String safeName(ItemStack is) {
        ResourceLocation rl = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(is.getItem());
        return (rl == null) ? "[NO NAME, WTF?!]" : rl.toString();
    }

    public boolean addUpgrade(BlockSide side, ItemStack is, @Nullable Player player, boolean abortIfExisting) {
        if (level == null || level.isClientSide)
            return false;

        Screen scr = getScreen(side);

        if (scr == null) {
            Log.error("Tried to add an upgrade on invalid screen on side %s", side.toString());
            return false;
        }

        if (!(is.getItem() instanceof IUpgrade)) {
            Log.error("Tried to add a non-upgrade item %s to screen (%s does not implement IUpgrade)",
                    safeName(is), is.getItem().getClass().getCanonicalName());
            return false;
        }

        if (scr.upgrades.size() >= 16) {
            Log.error("Can't insert upgrade %s in screen %s at %s: too many upgrades already!",
                    safeName(is), side.toString(), worldPosition.toString());
            return false;
        }

        IUpgrade itemAsUpgrade = (IUpgrade) is.getItem();

        if (abortIfExisting && scr.upgrades.stream().anyMatch(other -> itemAsUpgrade.isSameUpgrade(is, other)))
            return false;

        ItemStack isCopy = is.copy();
        isCopy.setCount(1);

        scr.upgrades.add(isCopy);
        sendScreenConfig(scr);
        itemAsUpgrade.onInstall(this, side, player, isCopy);
        playSoundAt(WDContent.SOUND_UPGRADE_ADD.get(), worldPosition, 1.0f, 1.0f);
        setChanged();
        return true;
    }

    public boolean hasUpgrade(BlockSide side, ItemStack is) {
        Screen scr = getScreen(side);

        if (scr == null || !(is.getItem() instanceof IUpgrade))
            return false;

        IUpgrade itemAsUpgrade = (IUpgrade) is.getItem();
        return scr.upgrades.stream().anyMatch(other -> itemAsUpgrade.isSameUpgrade(is, other));
    }

    public boolean hasUpgrade(BlockSide side, DefaultUpgrade du) {
        Screen scr = getScreen(side);
        return scr != null && scr.upgrades.stream().anyMatch(du::matches);
    }

    public void removeUpgrade(BlockSide side, ItemStack is, @Nullable Player player) {
        if (level == null || level.isClientSide)
            return;

        Screen scr = getScreen(side);

        if (scr == null) {
            Log.error("Tried to remove an upgrade on invalid screen on side %s", side.toString());
            return;
        }

        if (!(is.getItem() instanceof IUpgrade)) {
            Log.error("Tried to remove a non-upgrade item %s to screen", safeName(is));
            return;
        }

        int idxToRemove = -1;
        IUpgrade itemAsUpgrade = (IUpgrade) is.getItem();

        for (int i = 0; i < scr.upgrades.size(); i++) {
            if (itemAsUpgrade.isSameUpgrade(is, scr.upgrades.get(i))) {
                idxToRemove = i;
                break;
            }
        }

        if (idxToRemove >= 0) {
            dropUpgrade(scr.upgrades.get(idxToRemove), side, player);
            scr.upgrades.remove(idxToRemove);
            sendScreenConfig(scr);
            playSoundAt(WDContent.SOUND_UPGRADE_DEL.get(), worldPosition, 1.0f, 1.0f);
            setChanged();
        } else
            Log.warning("Tried to remove non-existing upgrade %s to screen %s at %s",
                    safeName(is), side.toString(), worldPosition.toString());
    }

    private void dropUpgrade(ItemStack is, BlockSide side, @Nullable Player ply) {
        if (!((IUpgrade) is.getItem()).onRemove(this, side, ply, is)) {
            boolean spawnDrop = true;

            if (ply != null) {
                if (ply.getAbilities().instabuild || ply.getInventory().add(is))
                    spawnDrop = false;
            }

            if (spawnDrop) {
                Vector3f pos = new Vector3f((float) worldPosition.getX(), (float) worldPosition.getY(), (float) worldPosition.getZ());
                pos.addMul(side.backward.toFloat(), 1.5f);

                level.addFreshEntity(new ItemEntity(level, (double) pos.x, (double) pos.y, (double) pos.z, is));
            }
        }
    }

    private Screen getScreenForLaserOp(BlockSide side, Player ply) {
        if (level == null || level.isClientSide)
            return null;

        Screen scr = getScreen(side);

        if (scr == null) {
            Log.error("Called laser operation on invalid screen on side %s", side.toString());
            return null;
        }

        if ((scr.rightsFor(ply) & ScreenRights.CLICK) == 0)
            return null;

        if (scr.upgrades.stream().noneMatch(DefaultUpgrade.LASER_MOUSE::matches)) {
            Log.error("Called laser operation on side %s, but it's missing the laser sensor upgrade", side.toString());
            return null;
        }

        return scr;
    }

    public void laserDownMove(BlockSide side, Player ply, Vector2i pos, boolean down) {
        Screen scr = getScreenForLaserOp(side, ply);

        if (scr == null)
            return;

        if (down) {
            if (getLaserUser(scr) == null) {
                scr.laserUser = ply;
                sendAround(S2CScreenUpdate.click(new Vector3i(worldPosition), side, S2CScreenUpdate.MOUSE_DOWN, pos));
            }
        } else if (getLaserUser(scr) == ply)
            sendAround(S2CScreenUpdate.click(new Vector3i(worldPosition), side, S2CScreenUpdate.MOUSE_MOVE, pos));
    }

    public void laserUp(BlockSide side, Player ply) {
        Screen scr = getScreenForLaserOp(side, ply);

        if (scr != null && getLaserUser(scr) == ply) {
            scr.laserUser = null;
            sendAround(S2CScreenUpdate.click(new Vector3i(worldPosition), side, S2CScreenUpdate.MOUSE_UP, null));
        }
    }

    public void onDestroy(@Nullable Player ply) {
        for (Screen scr : screens) {
            scr.upgrades.forEach(is -> dropUpgrade(is, scr.side, ply));
            scr.upgrades.clear();
        }

        sendAround(new S2CCloseGui(new Vector3i(worldPosition)));
    }

    public void setOwner(BlockSide side, Player newOwner) {
        if (level == null || level.isClientSide) {
            Log.error("Called ScreenBlockEntity.setOwner() on client...");
            return;
        }

        if (newOwner == null) {
            Log.error("Called ScreenBlockEntity.setOwner() with null owner");
            return;
        }

        Screen scr = getScreen(side);

        if (scr == null) {
            Log.error("Called ScreenBlockEntity.setOwner() on invalid screen on side %s", side.toString());
            return;
        }

        scr.owner = new NameUUIDPair(newOwner.getGameProfile());
        sendAround(S2CScreenUpdate.setOwner(new Vector3i(worldPosition), side, scr.owner));
        checkLaserUserRights(scr);
        setChanged();
    }

    public void setRotation(BlockSide side, Rotation rot) {
        Screen scr = getScreen(side);

        if (scr == null) {
            Log.error("Trying to change rotation of invalid screen on side %s", side.toString());
            return;
        }

        if (level != null && level.isClientSide) {
            boolean oldWasVertical = scr.rotation.isVertical;
            scr.rotation = rot;

            net.montoyo.wd.client.ClientProxy.get().screenUpdateRotationInGui(new Vector3i(worldPosition), side, rot);

            if (scr.browser != null && oldWasVertical != rot.isVertical) {
                scr.browser.close();
                scr.browser = null;
            }
        } else {
            scr.rotation = rot;
            sendAround(S2CScreenUpdate.setRotation(new Vector3i(worldPosition), side, rot.ordinal()));
            setChanged();
        }
    }

    public void evalJS(BlockSide side, String code) {
        Screen scr = getScreen(side);

        if (scr == null) {
            Log.error("Trying to run JS code on invalid screen on side %s", side.toString());
            return;
        }

        if (level != null && level.isClientSide) {
            if (scr.browser != null)
                scr.browser.runJS(code, "");
        } else
            sendAround(S2CScreenUpdate.runJs(new Vector3i(worldPosition), side, code));
    }

    public void setAutoVolume(BlockSide side, boolean av) {
        Screen scr = getScreen(side);

        if (scr == null) {
            Log.error("Trying to toggle auto-volume on invalid screen (side %s)", side.toString());
            return;
        }

        scr.autoVolume = av;

        if (level != null && level.isClientSide)
            net.montoyo.wd.client.ClientProxy.get().screenUpdateAutoVolumeInGui(new Vector3i(worldPosition), side, av);
        else {
            sendAround(S2CScreenUpdate.setAutoVolume(new Vector3i(worldPosition), side, av));
            setChanged();
        }
    }
}
