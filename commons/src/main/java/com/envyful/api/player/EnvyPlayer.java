package com.envyful.api.player;

import com.envyful.api.config.ConfigLocation;
import com.envyful.api.player.attribute.Attribute;
import com.envyful.api.player.attribute.PlayerAttribute;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 *
 * This interface is designed to provide basic useful
 * methods for all the different player implementations independent
 * of the platform details (i.e. auto-translates
 * all text sent to the player, and makes it less complicated to do
 * different functions such as sending titles etc.).
 * <br>
 * It also stores {@link PlayerAttribute} from the
 * plugin implementation that will include specific data from the
 * plugin / mod. The attributes stored by the
 * plugin's / manager's class as to allow each mod / plugin to have multiple
 * attributes for storing different sets of data.
 *
 * @param <T> The specific platform implementation of the player object.
 */
public interface EnvyPlayer<T> {

    UUID getUuid();

    String getName();

    T getParent();

    void message(Object... messages);

    void executeCommand(String command);

    void executeCommands(String... commands);

    void teleport(ConfigLocation location);

    List<Attribute<?>> getAttributes();

    <A extends Attribute<B>, B> A getAttribute(Class<A> attributeClass);

    void invalidateAttribute(Attribute<?> attribute);

    <A extends Attribute<B>, B> CompletableFuture<A> loadAttribute(
            Class<? extends A> attributeClass, B id);

    <A extends Attribute<?>> void setAttribute(A attribute);

}
