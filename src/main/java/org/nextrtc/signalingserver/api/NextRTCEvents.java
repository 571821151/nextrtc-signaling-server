package org.nextrtc.signalingserver.api;

import org.nextrtc.signalingserver.api.dto.NextRTCEvent;
import org.nextrtc.signalingserver.domain.Conversation;
import org.nextrtc.signalingserver.domain.EventContext;
import org.nextrtc.signalingserver.domain.InternalMessage;
import org.nextrtc.signalingserver.exception.Exceptions;

import javax.websocket.Session;

public enum NextRTCEvents {
    SESSION_OPENED,
    SESSION_CLOSED,
    CONVERSATION_CREATED,
    CONVERSATION_DESTROYED,
    UNEXPECTED_SITUATION,
    MEMBER_JOINED,
    MEMBER_LEFT,
    MEDIA_LOCAL_STREAM_REQUESTED,
    MEDIA_LOCAL_STREAM_CREATED,
    MEDIA_STREAMING,;

    public NextRTCEvent basedOn(InternalMessage message, Conversation conversation) {
        return EventContext.builder()
                .from(message.getFrom())
                .to(message.getTo())
                .custom(message.getCustom())
                .conversation(conversation)
                .type(this)
                .build();
    }

    public NextRTCEvent basedOn(EventContext.EventContextBuilder builder) {
        return builder
                .type(this)
                .build();
    }

    public NextRTCEvent occurFor(Session session, String reason) {
        return EventContext.builder()
                .from(() -> session)
                .type(this)
                .reason(reason)
                .build();
    }

    public NextRTCEvent occurFor(Session session) {
        return EventContext.builder()
                .type(this)
                .from(() -> session)
                .exception(Exceptions.UNKNOWN_ERROR.exception())
                .build();
    }
}
