package me.mastercapexd.auth.messenger.commands;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.bivashy.auth.api.AuthPlugin;
import com.bivashy.auth.api.account.Account;
import com.bivashy.auth.api.link.LinkType;
import com.bivashy.auth.api.link.user.entry.LinkEntryUser;

import me.mastercapexd.auth.link.LinkCommandActorWrapper;
import revxrsal.commands.annotation.Default;
import revxrsal.commands.annotation.Dependency;
import revxrsal.commands.orphan.OrphanCommand;

public class AccountEnterAcceptCommand implements OrphanCommand {
    public static final String CONFIGURATION_KEY = "enter-accept";
    @Dependency
    private AuthPlugin plugin;

    @Default
    public void onAccept(LinkCommandActorWrapper actorWrapper, LinkType linkType, @Default("all") String acceptPlayerName) {
        List<LinkEntryUser> accounts = plugin.getLinkEntryBucket().getLinkUsers(entryUser -> {
            if (!entryUser.getLinkType().equals(linkType))
                return false;

            if (!entryUser.getLinkUserInfo().getIdentificator().equals(actorWrapper.userId()))
                return false;

            Duration confirmationSecondsPassed = Duration.of(System.currentTimeMillis() - entryUser.getConfirmationStartTime(), ChronoUnit.MILLIS);

            if (confirmationSecondsPassed.getSeconds() > linkType.getSettings().getEnterSettings().getEnterDelay())
                return false;

            if (!acceptPlayerName.equals("all")) // If player not default value
                return entryUser.getAccount().getName().equalsIgnoreCase(acceptPlayerName); // Check if entryUser name
            // equals to accept player
            return true;
        });
        if (accounts.isEmpty()) {
            actorWrapper.reply(linkType.getLinkMessages().getMessage("enter-no-accounts"));
            return;
        }
        accounts.forEach((entryUser) -> {
            entryUser.setConfirmed(true);
            Account account = entryUser.getAccount();
            account.getPlayer().ifPresent(player -> player.sendMessage(linkType.getServerMessages().getStringMessage("enter-confirmed",
                    linkType.newMessageContext(account))));
            account.nextAuthenticationStep(plugin.getAuthenticationContextFactoryBucket().createContext(account));
            plugin.getLinkEntryBucket().removeLinkUser(entryUser);

            actorWrapper.reply(linkType.getLinkMessages().getMessage("enter-accepted", linkType.newMessageContext(account)));
        });
    }
}
