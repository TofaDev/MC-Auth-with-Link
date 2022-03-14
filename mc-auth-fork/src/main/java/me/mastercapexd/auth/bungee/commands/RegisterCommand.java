package me.mastercapexd.auth.bungee.commands;

import me.mastercapexd.auth.account.Account;
import me.mastercapexd.auth.authentication.step.AuthenticationStep;
import me.mastercapexd.auth.authentication.step.steps.RegisterAuthenticationStep;
import me.mastercapexd.auth.bungee.AuthPlugin;
import me.mastercapexd.auth.bungee.commands.annotations.AuthenticationAccount;
import me.mastercapexd.auth.bungee.commands.annotations.AuthenticationStepCommand;
import me.mastercapexd.auth.bungee.commands.parameters.NewPassword;
import me.mastercapexd.auth.config.PluginConfig;
import me.mastercapexd.auth.storage.AccountStorage;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Default;
import revxrsal.commands.annotation.Dependency;

@Command({"reg","register"})
public class RegisterCommand {

	@Dependency
	private AuthPlugin plugin;
	@Dependency
	private PluginConfig config;
	@Dependency
	private AccountStorage accountStorage;

	@Default
	@AuthenticationStepCommand(stepName = RegisterAuthenticationStep.STEP_NAME)
	public void register(ProxiedPlayer player, @AuthenticationAccount Account account, NewPassword password) {
		AuthenticationStep currentAuthenticationStep = account.getCurrentAuthenticationStep();
		currentAuthenticationStep.getAuthenticationStepContext().setCanPassToNextStep(true);
		String stepName = plugin.getConfig()
				.getAuthenticationStepName(account.getCurrentConfigurationAuthenticationStepCreatorIndex());
		
		if(account.getHashType()!=config.getActiveHashType()) 
			account.setHashType(config.getActiveHashType());
		account.setPasswordHash(account.getHashType().hash(password.getNewPassword()));
		
		accountStorage.saveOrUpdateAccount(account);
		
		account.nextAuthenticationStep(
				plugin.getAuthenticationContextFactoryDealership().createContext(stepName, account));
		
		player.sendMessage(config.getBungeeMessages().getMessage("register-success"));
	}
}