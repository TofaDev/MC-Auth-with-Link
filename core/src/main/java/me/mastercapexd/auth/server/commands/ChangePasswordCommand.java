package me.mastercapexd.auth.server.commands;

import com.bivashy.auth.api.AuthPlugin;
import com.bivashy.auth.api.config.PluginConfig;
import com.bivashy.auth.api.database.AccountDatabase;
import com.bivashy.auth.api.event.AccountTryChangePasswordEvent;
import com.bivashy.auth.api.server.player.ServerPlayer;

import io.github.revxrsal.eventbus.PostResult;
import me.mastercapexd.auth.server.commands.parameters.DoublePassword;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Default;
import revxrsal.commands.annotation.Dependency;

@Command({"passchange", "changepass", "changepassword"})
public class ChangePasswordCommand {
    @Dependency
    private AuthPlugin plugin;
    @Dependency
    private PluginConfig config;
    @Dependency
    private AccountDatabase accountStorage;

    @Default
    public void changePlayerPassword(ServerPlayer sender, DoublePassword password) {
        String id = config.getActiveIdentifierType().getId(sender);
        accountStorage.getAccount(id).thenAcceptAsync(account -> {
            if (account == null || !account.isRegistered()) {
                sender.sendMessage(config.getServerMessages().getMessage("account-not-found"));
                return;
            }
            boolean isWrongPassword = !account.getHashType().matches(password.getOldPassword(), account.getPasswordHash());
            PostResult<AccountTryChangePasswordEvent> tryChangePasswordEventPostResult = plugin.getEventBus()
                    .publish(AccountTryChangePasswordEvent.class, account, false, !isWrongPassword).join();
            if (tryChangePasswordEventPostResult.getEvent().isCancelled())
                return;

            if (isWrongPassword) {
                sender.sendMessage(config.getServerMessages().getMessage("wrong-old-password"));
                return;
            }

            if (account.getHashType() != config.getActiveHashType())
                account.setHashType(config.getActiveHashType());

            account.setPasswordHash(account.getHashType().hash(password.getNewPassword()));
            accountStorage.saveOrUpdateAccount(account);
            sender.sendMessage(config.getServerMessages().getMessage("change-success"));
        });
    }
}