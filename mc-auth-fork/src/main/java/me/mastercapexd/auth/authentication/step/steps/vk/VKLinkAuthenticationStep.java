package me.mastercapexd.auth.authentication.step.steps.vk;

import me.mastercapexd.auth.Auth;
import me.mastercapexd.auth.account.Account;
import me.mastercapexd.auth.account.factories.AccountFactory;
import me.mastercapexd.auth.authentication.step.AbstractAuthenticationStep;
import me.mastercapexd.auth.authentication.step.AuthenticationStep;
import me.mastercapexd.auth.authentication.step.context.AuthenticationStepContext;
import me.mastercapexd.auth.authentication.step.creators.AbstractAuthenticationStepCreator;
import me.mastercapexd.auth.bungee.AuthPlugin;
import me.mastercapexd.auth.link.entryuser.LinkEntryUser;
import me.mastercapexd.auth.link.entryuser.vk.VKLinkEntryUser;
import me.mastercapexd.auth.link.user.LinkUser;
import me.mastercapexd.auth.link.vk.VKLinkType;
import me.mastercapexd.auth.proxy.ProxyPlugin;

public class VKLinkAuthenticationStep extends AbstractAuthenticationStep {
	private static final ProxyPlugin PLUGIN = ProxyPlugin.instance();
	public static final String STEP_NAME = "VK_LINK";
	private final LinkEntryUser entryUser;

	public VKLinkAuthenticationStep(AuthenticationStepContext context) {
		super(STEP_NAME, context);
		entryUser = new VKLinkEntryUser(context.getAccount());
	}

	@Override
	public boolean shouldPassToNextStep() {
		return entryUser.isConfirmed();
	}

	@Override
	public boolean shouldSkip() {
		Account account = authenticationStepContext.getAccount();
		if (account.isSessionActive(PLUGIN.getConfig().getSessionDurability()))
			return true;
		LinkUser linkUser = account.findFirstLinkUser(VKLinkType.LINK_USER_FILTER).orElse(null);

		if (linkUser == null || linkUser.getLinkUserInfo() == null
				|| linkUser.getLinkUserInfo().getIdentificator().asNumber() == AccountFactory.DEFAULT_VK_ID
				|| !PLUGIN.getConfig().getVKSettings().isEnabled()
				|| !linkUser.getLinkUserInfo().getConfirmationState().shouldSendConfirmation())
			return true;

		if (Auth.getLinkEntryAuth().hasLinkUser(account.getId(), VKLinkType.getInstance()))
			return true;

		Auth.getLinkEntryAuth().addLinkUser(entryUser);
		return false;
	}

	public static class VKLinkAuthenticationStepCreator extends AbstractAuthenticationStepCreator {
		public VKLinkAuthenticationStepCreator() {
			super(STEP_NAME);
		}

		@Override
		public AuthenticationStep createNewAuthenticationStep(AuthenticationStepContext context) {
			return new VKLinkAuthenticationStep(context);
		}
	}
}
