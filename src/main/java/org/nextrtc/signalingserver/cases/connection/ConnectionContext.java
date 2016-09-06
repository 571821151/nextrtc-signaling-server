package org.nextrtc.signalingserver.cases.connection;

import org.joda.time.DateTime;
import org.nextrtc.signalingserver.Names;
import org.nextrtc.signalingserver.api.NextRTCEventBus;
import org.nextrtc.signalingserver.api.NextRTCEvents;
import org.nextrtc.signalingserver.domain.InternalMessage;
import org.nextrtc.signalingserver.domain.Member;
import org.nextrtc.signalingserver.domain.Signal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.function.BiPredicate;

@Component
@Scope("prototype")
public class ConnectionContext {

    @Value(Names.MAX_CONNECTION_SETUP_TIME)
    private int maxConnectionSetupTime;

    private ConnectionState state = ConnectionState.NOT_INITIALIZED;
    private DateTime lastUpdated = DateTime.now();

    @Autowired
    @Qualifier(Names.EVENT_BUS)
    private NextRTCEventBus bus;

    private Member master;
    private Member slave;
    private BiPredicate<Member, Member> filter;


    public ConnectionContext(Member master, Member slave, BiPredicate<Member, Member> filter) {
        this.master = master;
        this.slave = slave;
        this.filter = filter;
    }


    public void process(InternalMessage message) {
        if (is(message, ConnectionState.OFFER_REQUESTED)) {
            answerRequest(message);
            setState(ConnectionState.ANSWER_REQUESTED);
        } else if (is(message, ConnectionState.ANSWER_REQUESTED)) {
            finalize(message);
            setState(ConnectionState.EXCHANGE_CANDIDATES);
        } else if (is(message, ConnectionState.EXCHANGE_CANDIDATES)) {
            if (filter.test(master, message.getFrom())) {
                exchangeCandidates(message);
                setState(ConnectionState.EXCHANGE_CANDIDATES);
            }
        }
    }


    private void exchangeCandidates(InternalMessage message) {
        InternalMessage.create()//
                .from(message.getFrom())//
                .to(message.getTo())//
                .signal(Signal.CANDIDATE)//
                .content(message.getContent())//
                .build()//
                .send();
    }


    private void finalize(InternalMessage message) {
        InternalMessage.create()//
                .from(slave)//
                .to(master)//
                .signal(Signal.FINALIZE)//
                .content(message.getContent())//
                .build()//
                .send();
        bus.post(NextRTCEvents.MEDIA_LOCAL_STREAM_CREATED.occurFor(slave.getSession()));
        bus.post(NextRTCEvents.MEDIA_STREAMING.occurFor(master.getSession()));
        bus.post(NextRTCEvents.MEDIA_STREAMING.occurFor(slave.getSession()));
    }


    private void answerRequest(InternalMessage message) {
        bus.post(NextRTCEvents.MEDIA_LOCAL_STREAM_CREATED.occurFor(master.getSession()));
        InternalMessage.create()//
                .from(master)//
                .to(slave)//
                .signal(Signal.ANSWER_REQUEST)//
                .content(message.getContent())//
                .build()//
                .send();
        bus.post(NextRTCEvents.MEDIA_LOCAL_STREAM_REQUESTED.occurFor(slave.getSession()));
    }

    private boolean is(InternalMessage message, ConnectionState state) {
        return state.equals(this.state) && state.isValid(message);
    }

    public void begin() {
        InternalMessage.create()//
                .from(slave)//
                .to(master)//
                .signal(Signal.OFFER_REQUEST)
                .build()//
                .send();
        setState(ConnectionState.OFFER_REQUESTED);
        bus.post(NextRTCEvents.MEDIA_LOCAL_STREAM_REQUESTED.occurFor(master.getSession()));
    }

    private void setState(ConnectionState state) {
        this.state = state;
        lastUpdated = DateTime.now();
    }

    public boolean isCurrent() {
        return lastUpdated.plusSeconds(maxConnectionSetupTime).isAfter(DateTime.now());
    }

    public Member getMaster() {
        return master;
    }

    public Member getSlave() {
        return slave;
    }

    public ConnectionState getState() {
        return state;
    }
}
