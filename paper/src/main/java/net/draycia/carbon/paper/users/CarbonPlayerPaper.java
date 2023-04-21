/*
 * CarbonChat
 *
 * Copyright (c) 2023 Josua Parks (Vicarious)
 *                    Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.draycia.carbon.paper.users;

import io.papermc.paper.event.player.AsyncChatEvent;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import net.draycia.carbon.api.channels.ChatChannel;
import net.draycia.carbon.api.users.CarbonPlayer;
import net.draycia.carbon.api.util.InventorySlot;
import net.draycia.carbon.common.users.CarbonPlayerCommon;
import net.draycia.carbon.common.users.WrappedCarbonPlayer;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.MetadataValue;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.jetbrains.annotations.NotNull;

@DefaultQualifier(NonNull.class)
public final class CarbonPlayerPaper extends WrappedCarbonPlayer implements ForwardingAudience.Single {

    private final CarbonPlayerCommon carbonPlayerCommon;

    public CarbonPlayerPaper(final CarbonPlayerCommon carbonPlayerCommon) {
        this.carbonPlayerCommon = carbonPlayerCommon;
    }

    private Optional<Player> player() {
        return Optional.ofNullable(Bukkit.getPlayer(this.carbonPlayerCommon.uuid()));
    }

    @Override
    public CarbonPlayerCommon carbonPlayerCommon() {
        return this.carbonPlayerCommon;
    }

    @Override
    public @NotNull Audience audience() {
        return this.player().map(player -> (Audience) player).orElse(Audience.empty());
    }

    @Override
    public double distanceSquaredFrom(final CarbonPlayer other) {
        if (this.player().isEmpty()) {
            return -1;
        }

        final @Nullable Player player = this.player().orElse(null);
        final @Nullable Player otherPlayer = Bukkit.getPlayer(other.uuid());

        if (player == null || otherPlayer == null) {
            return -1;
        }

        return player.getLocation().distanceSquared(otherPlayer.getLocation());
    }

    @Override
    public boolean sameWorldAs(final CarbonPlayer other) {
        if (this.player().isEmpty()) {
            return false;
        }

        final Optional<Player> player = this.player();
        final @Nullable Player otherPlayer = Bukkit.getPlayer(other.uuid());

        if (player.isEmpty() || otherPlayer == null) {
            return false;
        }

        return player.get().getWorld().equals(otherPlayer.getWorld());
    }

    @Override
    public void displayName(final @Nullable Component displayName) {
        this.carbonPlayerCommon.displayName(displayName);

        this.player().ifPresent(player -> {
            // Update player's name in chat
            player.displayName(displayName);

            // Update player's name in the tab player list
            player.playerListName(displayName);
        });
    }

    @Override
    public @Nullable Component createItemHoverComponent(final InventorySlot slot) {
        final Optional<Player> player = this.player(); // This is temporary (it's not)

        if (player.isEmpty()) {
            return null;
        }

        final EquipmentSlot equipmentSlot;

        if (slot.equals(InventorySlot.MAIN_HAND)) {
            equipmentSlot = EquipmentSlot.HAND;
        } else if (slot.equals(InventorySlot.OFF_HAND)) {
            equipmentSlot = EquipmentSlot.OFF_HAND;
        } else if (slot.equals(InventorySlot.HELMET)) {
            equipmentSlot = EquipmentSlot.HEAD;
        } else if (slot.equals(InventorySlot.CHEST)) {
            equipmentSlot = EquipmentSlot.CHEST;
        } else if (slot.equals(InventorySlot.LEGS)) {
            equipmentSlot = EquipmentSlot.LEGS;
        } else if (slot.equals(InventorySlot.BOOTS)) {
            equipmentSlot = EquipmentSlot.FEET;
        } else {
            return null;
        }

        final @Nullable EntityEquipment equipment = player.get().getEquipment();

        if (equipment == null) {
            return null;
        }

        final @Nullable ItemStack itemStack = equipment.getItem(equipmentSlot);

        if (itemStack == null || itemStack.getType().isAir()) {
            return null;
        }

        return itemStack.displayName();
    }

    @Override
    public @Nullable Locale locale() {
        return this.player().map(Player::locale).orElse(null);
    }

    @Override
    public void sendMessageAsPlayer(final String message) {
        // TODO: ensure method is not executed from main thread
        // bukkit doesn't like that
        this.player().ifPresent(player -> player.chat(message));
    }

    @Override
    public boolean speechPermitted(final String message) {
        // ...........
        return new AsyncPlayerChatEvent(!Bukkit.isPrimaryThread(), this.player().get(), message, Set.of()).callEvent()
            && new AsyncChatEvent(!Bukkit.isPrimaryThread(), this.player().get(), Set.of(), ((player, component, component1, audience) -> component), Component.text(message), Component.text(message)).callEvent();
    }

    @Override
    public boolean online() {
        return this.player().isPresent();
    }

    @Override
    public boolean vanished() {
        return this.hasVanishMeta();
    }

    @Override
    public List<Key> leftChannels() {
        return this.carbonPlayerCommon.leftChannels();
    }

    @Override
    public void joinChannel(final ChatChannel channel) {
        this.carbonPlayerCommon.joinChannel(channel);
    }

    @Override
    public void leaveChannel(final ChatChannel channel) {
        this.carbonPlayerCommon.leaveChannel(channel);
    }

    // Supported by PremiumVanish, SuperVanish, VanishNoPacket
    private boolean hasVanishMeta() {
        return this.player().stream()
            .map(player -> player.getMetadata("vanished"))
            .flatMap(Collection::stream)
            .filter(value -> value.value() instanceof Boolean)
            .anyMatch(MetadataValue::asBoolean);
    }

    public @Nullable Player bukkitPlayer() {
        return Bukkit.getPlayer(this.uuid());
    }

    @Override
    public boolean equals(final @Nullable Object other) {
        if (other == null || this.getClass() != other.getClass()) {
            return false;
        }

        final CarbonPlayerPaper that = (CarbonPlayerPaper) other;

        return this.carbonPlayerCommon.equals(that.carbonPlayerCommon);
    }

    @Override
    public int hashCode() {
        return this.carbonPlayerCommon.hashCode();
    }

}
