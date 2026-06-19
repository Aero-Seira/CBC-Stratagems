package com.aeroseira.cbcstratagems.player;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.util.INBTSerializable;

public final class PlayerStratagemData implements INBTSerializable<CompoundTag> {
    private static final String UNLOCKED_KEY = "unlocked_stratagems";
    private static final String COOLDOWNS_KEY = "cooldowns";
    private static final String COOLDOWN_ID_KEY = "id";
    private static final String COOLDOWN_END_KEY = "end_time";

    private final Set<ResourceLocation> unlockedStratagems = new LinkedHashSet<>();
    private final Map<ResourceLocation, Long> cooldowns = new LinkedHashMap<>();

    public boolean isUnlocked(ResourceLocation stratagemId) {
        return unlockedStratagems.contains(stratagemId);
    }

    public boolean unlock(ResourceLocation stratagemId) {
        return unlockedStratagems.add(stratagemId);
    }

    public Set<ResourceLocation> unlockedStratagems() {
        return Collections.unmodifiableSet(unlockedStratagems);
    }

    public long getCooldownEnd(ResourceLocation stratagemId) {
        return cooldowns.getOrDefault(stratagemId, 0L);
    }

    public void startCooldown(ResourceLocation stratagemId, long endTime) {
        if (endTime <= 0L) {
            cooldowns.remove(stratagemId);
        } else {
            cooldowns.put(stratagemId, endTime);
        }
    }

    public Map<ResourceLocation, Long> cooldowns() {
        return Collections.unmodifiableMap(cooldowns);
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();

        ListTag unlockedTag = new ListTag();
        for (ResourceLocation stratagemId : unlockedStratagems) {
            unlockedTag.add(StringTag.valueOf(stratagemId.toString()));
        }
        tag.put(UNLOCKED_KEY, unlockedTag);

        ListTag cooldownsTag = new ListTag();
        for (Map.Entry<ResourceLocation, Long> entry : cooldowns.entrySet()) {
            CompoundTag cooldownTag = new CompoundTag();
            cooldownTag.putString(COOLDOWN_ID_KEY, entry.getKey().toString());
            cooldownTag.putLong(COOLDOWN_END_KEY, entry.getValue());
            cooldownsTag.add(cooldownTag);
        }
        tag.put(COOLDOWNS_KEY, cooldownsTag);

        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        unlockedStratagems.clear();
        cooldowns.clear();

        ListTag unlockedTag = tag.getList(UNLOCKED_KEY, Tag.TAG_STRING);
        for (int i = 0; i < unlockedTag.size(); i++) {
            ResourceLocation.read(unlockedTag.getString(i)).result().ifPresent(unlockedStratagems::add);
        }

        ListTag cooldownsTag = tag.getList(COOLDOWNS_KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < cooldownsTag.size(); i++) {
            CompoundTag cooldownTag = cooldownsTag.getCompound(i);
            ResourceLocation.read(cooldownTag.getString(COOLDOWN_ID_KEY))
                    .result()
                    .ifPresent(stratagemId -> cooldowns.put(stratagemId, cooldownTag.getLong(COOLDOWN_END_KEY)));
        }
    }
}
