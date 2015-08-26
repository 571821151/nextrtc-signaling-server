package org.nextrtc.signalingserver.cases;

import static org.nextrtc.signalingserver.api.NextRTCEvents.MEMBER_JOINDED;
import static org.nextrtc.signalingserver.exception.Exceptions.CONVERSATION_NOT_FOUND;

import org.nextrtc.signalingserver.api.NextRTCEventBus;
import org.nextrtc.signalingserver.domain.Conversation;
import org.nextrtc.signalingserver.domain.InternalMessage;
import org.nextrtc.signalingserver.repository.Conversations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class JoinConversation {

	@Autowired
	@Qualifier("nextRTCEventBus")
	private NextRTCEventBus eventBus;

	@Autowired
	private Conversations conversations;

	public void execute(InternalMessage message) {
		Conversation conversation = findConversationToJoin(message);

		conversation.join(message.getFrom());

        sendEventMemberJoinedFrom(message, conversation);
	}

    private void sendEventMemberJoinedFrom(InternalMessage message, Conversation conversation) {
        eventBus.post(MEMBER_JOINDED.basedOn(message, conversation));
	}

	private Conversation findConversationToJoin(InternalMessage message) {
		return conversations.findBy(message.getContent()).orElseThrow(() -> CONVERSATION_NOT_FOUND.exception());
	}

}
