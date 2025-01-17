package com.envyful.api.forge.gui.type;

import com.envyful.api.config.type.ConfigInterface;
import com.envyful.api.config.type.ConfigItem;
import com.envyful.api.config.type.ExtendedConfigItem;
import com.envyful.api.forge.chat.UtilChatColour;
import com.envyful.api.forge.config.UtilConfigInterface;
import com.envyful.api.forge.config.UtilConfigItem;
import com.envyful.api.forge.gui.item.PositionableItem;
import com.envyful.api.forge.player.ForgeEnvyPlayer;
import com.envyful.api.gui.factory.GuiFactory;
import com.envyful.api.gui.item.Displayable;
import com.envyful.api.gui.pane.Pane;
import com.envyful.api.player.EnvyPlayer;
import com.envyful.api.player.PlayerManager;
import com.envyful.api.text.Placeholder;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.minecraft.item.ItemStack;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;

/**
 *
 * A utility class for building generic Confirmation UIs
 *
 */
public class ConfirmationUI {

    /**
     *
     * Opens the UI using the builder as the settings
     *
     * @param builder The UI settings
     */
    private static void open(Builder builder) {
        ConfirmConfig config = builder.confirmConfig;
        Pane pane = GuiFactory.paneBuilder()
                .topLeftX(0)
                .topLeftY(0)
                .width(9)
                .height(config.getGuiSettings().getHeight())
                .build();

        Placeholder[] placeholders = builder.placeholders.toArray(new Placeholder[0]);

        UtilConfigInterface.fillBackground(pane, config.getGuiSettings(), placeholders);

        UtilConfigItem.builder()
                .clickHandler(builder.confirmHandler)
                .extendedConfigItem((ForgeEnvyPlayer) builder.player, pane, config.getAcceptItem(), placeholders);

        UtilConfigItem.builder()
                .clickHandler(builder.returnHandler)
                .extendedConfigItem((ForgeEnvyPlayer) builder.player, pane, config.getDeclineItem(), placeholders);

        if (builder.descriptionItem != null) {
            pane.set(config.getDescriptionPosition() % 9, config.getDescriptionPosition() / 9,
                     GuiFactory.displayable(builder.descriptionItem)
            );
        }

        for (ExtendedConfigItem displayItem : builder.displayConfigItems) {
            UtilConfigItem.builder().extendedConfigItem((ForgeEnvyPlayer) builder.player, pane, displayItem, placeholders);
        }

        for (PositionableItem displayItem : builder.displayItems) {
            pane.set(displayItem.getPosX(), displayItem.getPosY(), GuiFactory.displayable(displayItem.getItemStack()));
        }

        GuiFactory.guiBuilder()
                .setPlayerManager(builder.playerManager)
                .addPane(pane)
                .height(config.getGuiSettings().getHeight())
                .title(UtilChatColour.colour(config.getGuiSettings().getTitle()))
                .build().open(builder.player);
    }

    /**
     *
     * Gets a new instance of the Builder class
     *
     * @return The builder class
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     *
     * For building the settings to the confirmation UI
     *
     */
    public static class Builder {

        private EnvyPlayer<?> player;
        private ItemStack descriptionItem;
        private ConfirmConfig confirmConfig;
        private PlayerManager<?, ?> playerManager;
        private BiConsumer<EnvyPlayer<?>, Displayable.ClickType> returnHandler;
        private BiConsumer<EnvyPlayer<?>, Displayable.ClickType> confirmHandler;
        private List<ExtendedConfigItem> displayConfigItems = Lists.newArrayList();
        private List<PositionableItem> displayItems = Lists.newArrayList();
        private List<Placeholder> placeholders = Lists.newArrayList();

        protected Builder() {}

        public Builder player(EnvyPlayer<?> player) {
            this.player = player;
            return this;
        }

        public Builder playerManager(PlayerManager<?, ?> playerManager) {
            this.playerManager = playerManager;
            return this;
        }

        public Builder descriptionItem(ItemStack descriptionItem) {
            this.descriptionItem = descriptionItem.copy();
            return this;
        }

        public Builder config(ConfirmConfig config) {
            this.confirmConfig = config;
            return this;
        }

        public Builder returnHandler(BiConsumer<EnvyPlayer<?>, Displayable.ClickType> returnHandler) {
            this.returnHandler = returnHandler;
            return this;
        }

        public Builder confirmHandler(BiConsumer<EnvyPlayer<?>, Displayable.ClickType> confirmHandler) {
            this.confirmHandler = confirmHandler;
            return this;
        }

        public Builder displayConfigItems(List<ExtendedConfigItem> displayConfigItems) {
            this.displayConfigItems.addAll(displayConfigItems);
            return this;
        }

        public Builder displayConfigItem(ExtendedConfigItem displayConfigItem) {
            this.displayConfigItems.add(displayConfigItem);
            return this;
        }

        public Builder displayConfigItems(ExtendedConfigItem... displayConfigItems) {
            this.displayConfigItems.addAll(Arrays.asList(displayConfigItems));
            return this;
        }

        public Builder displayItems(List<PositionableItem> displayItems) {
            this.displayItems.addAll(displayItems);
            return this;
        }

        public Builder displayItem(PositionableItem displayItem) {
            this.displayItems.add(displayItem);
            return this;
        }

        public Builder displayItems(PositionableItem... displayItems) {
            this.displayItems.addAll(Arrays.asList(displayItems));
            return this;
        }

        public Builder transformers(List<Placeholder> transformers) {
            this.placeholders.addAll(transformers);
            return this;
        }

        public Builder transformer(Placeholder transformer) {
            this.placeholders.add(transformer);
            return this;
        }

        public Builder transformers(Placeholder... transformers) {
            this.placeholders.addAll(Arrays.asList(transformers));
            return this;
        }

        public void open() {
            if (this.player == null || this.confirmConfig == null || this.playerManager == null) {
                return;
            }

            ConfirmationUI.open(this);
        }
    }

    /**
     *
     * The config settings for the UI
     *
     */
    @ConfigSerializable
    public static class ConfirmConfig {

        private ConfigInterface guiSettings = new ConfigInterface(
                "UltimatePokeBuilder", 3, "BLOCK",
                ImmutableMap.of("one", ConfigItem.builder()
                                .type("minecraft:black_stained_glass_pane")
                                .amount(1)
                                .name(" ")
                        .build())
        );

        private ExtendedConfigItem declineItem = new ExtendedConfigItem("minecraft:red_wool", 1, (byte) 14, "&c&lDECLINE",
                                                                    Lists.newArrayList(), 2, 1, Maps.newHashMap());


        private ExtendedConfigItem acceptItem = new ExtendedConfigItem("minecraft:lime_wool", 1, (byte) 5,
                                                                               "&a&lACCEPT",
                                                                                Lists.newArrayList(), 6, 1,
                                                                               Maps.newHashMap());

        private int descriptionPosition = 13;

        public ConfirmConfig() {
        }

        public ConfigInterface getGuiSettings() {
            return this.guiSettings;
        }

        public ExtendedConfigItem getDeclineItem() {
            return this.declineItem;
        }

        public ExtendedConfigItem getAcceptItem() {
            return this.acceptItem;
        }

        public int getDescriptionPosition() {
            return this.descriptionPosition;
        }
    }
}
