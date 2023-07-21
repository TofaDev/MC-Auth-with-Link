package com.bivashy.auth.api.event;

import com.bivashy.auth.api.event.base.AccountEvent;
import com.bivashy.auth.api.event.base.CancellableEvent;
import com.bivashy.auth.api.event.base.LinkEvent;
import com.bivashy.auth.api.link.user.info.LinkUserIdentificator;
import com.bivashy.auth.api.shared.commands.MessageableCommandActor;

import io.github.revxrsal.eventbus.gen.Index;
import io.github.revxrsal.eventbus.gen.RequireNonNull;

/**
 * Called when player uses /unlink, /googleunlink, or similar commands that unlinks LinkUser from account. Cancel results preventing unlinking
 */
public interface AccountUnlinkEvent extends AccountEvent, CancellableEvent, LinkEvent {
    @Index(4)
    LinkUserIdentificator getIdentificator();

    @Index(4)
    void setIdentificator(@RequireNonNull LinkUserIdentificator identificator);

    @Index(5)
    MessageableCommandActor getActor();
}
