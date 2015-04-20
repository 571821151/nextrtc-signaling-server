package org.nextrtc.signalingserver.domain.signal;

import lombok.extern.log4j.Log4j;

import org.nextrtc.signalingserver.api.annotation.NextRTCEvents;
import org.nextrtc.signalingserver.domain.Conversation;
import org.nextrtc.signalingserver.domain.InternalMessage;
import org.nextrtc.signalingserver.exception.Exceptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.google.common.base.Optional;
import com.google.common.eventbus.EventBus;

@Log4j
public abstract class AbstractSignal implements Signal {

	@Autowired
	@Qualifier("nextRTCEventBus")
	private EventBus eventBus;

	@Override
	public boolean is(String incomming) {
		return name().equalsIgnoreCase(incomming);
	}

	@Override
	public void executeMessage(InternalMessage message) {
		postEvent(before(), message);
		try {
			execute(message);
		} catch (Exception reason) {
			postEvent(Optional.of(error()), message);
			log.debug(reason.getMessage() + " " + message);
			throw reason;
		}
		postEvent(after(), message);
	}

	private void postEvent(Optional<NextRTCEvents> event, InternalMessage message) {
		if (event.isPresent()) {
			eventBus.post(event.get().basedOn(message));
		}
	}

	protected abstract void execute(InternalMessage message);

	protected Optional<NextRTCEvents> before() {
		return Optional.absent();
	}

	protected Optional<NextRTCEvents> after() {
		return Optional.absent();
	}

	protected NextRTCEvents error() {
		return NextRTCEvents.UNEXPECTED_SITUATION;
	}

	protected Conversation checkPrecondition(InternalMessage message, Optional<Conversation> conversation) {
		if (!conversation.isPresent()) {
			throw Exceptions.CONVERSATION_NOT_FOUND.exception();
		}
		if (!conversation.get().has(message.getTo())) {
			throw Exceptions.INVALID_RECIPIENT.exception();
		}
		return conversation.get();
	}
}
