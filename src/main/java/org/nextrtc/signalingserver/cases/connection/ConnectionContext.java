package org.nextrtc.signalingserver.cases.connection;

import lombok.Getter;

import org.joda.time.DateTime;
import org.nextrtc.signalingserver.domain.InternalMessage;
import org.nextrtc.signalingserver.domain.Member;
import org.nextrtc.signalingserver.domain.Signal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class ConnectionContext {

    @Value("${nextrtc.max_connection_setup_time:30}")
    private int maxConnectionSetupTime;

	private ConnectionState state = ConnectionState.NOT_INITIALIZED;
    private DateTime lastUpdated = DateTime.now();

    @Getter
    private Member master;
    @Getter
    private Member slave;


	public ConnectionContext(Member master, Member slave) {
		this.master = master;
		this.slave = slave;
	}


	public void process(InternalMessage message) {
		if (is(message, ConnectionState.OFFER_REQUESTED)) {
			answerRequest(message);
            setState(ConnectionState.ANSWER_REQUESTED);
		} else if (is(message, ConnectionState.ANSWER_REQUESTED)) {
			finalize(message);
            setState(ConnectionState.EXCHANGE_CANDIDATES);
		} else if (is(message, ConnectionState.EXCHANGE_CANDIDATES)) {
			exchangeCandidates(message);
            setState(ConnectionState.EXCHANGE_CANDIDATES);
		}
	}


	private void exchangeCandidates(InternalMessage message) {
		InternalMessage.create()//
				.from(message.getFrom())//
				.to(message.getTo())//
				.signal(Signal.CANDIDATE)//
				.content(message.getContent())//
				.build()//
				.post();
	}


	private void finalize(InternalMessage message) {
		InternalMessage.create()//
                .from(slave)//
                .to(master)//
				.signal(Signal.FINALIZE)//
				.content(message.getContent())//
				.build()//
				.post();
	}


	private void answerRequest(InternalMessage message) {
		InternalMessage.create()//
                .from(master)//
                .to(slave)//
				.signal(Signal.ANSWER_REQUEST)//
				.content(message.getContent())//
				.build()//
				.post();
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
				.post();
        setState(ConnectionState.OFFER_REQUESTED);
	}

    private void setState(ConnectionState state) {
        this.state = state;
        lastUpdated = DateTime.now();
    }

    public boolean isCurrent() {
        return lastUpdated.plusSeconds(maxConnectionSetupTime).isAfter(DateTime.now());
    }
}
