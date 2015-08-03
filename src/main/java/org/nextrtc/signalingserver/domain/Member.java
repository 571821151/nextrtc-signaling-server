package org.nextrtc.signalingserver.domain;

import static lombok.AccessLevel.PRIVATE;

import java.util.concurrent.ScheduledFuture;

import javax.websocket.Session;

import lombok.Data;
import lombok.Getter;
import lombok.experimental.Builder;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

@Data
@Builder(builderMethodName = "create")
public class Member {

	private String id;
	private Session session;

	@Getter(PRIVATE)
	private ScheduledFuture<?> ping;

	private Member(String id, Session session, ScheduledFuture<?> ping) {
		this.id = session.getId();
		this.session = session;
		this.ping = ping;
	}

	public void markLeft() {
		ping.cancel(true);
	}
	@Override
	public String toString() {
		return String.format("%s (%s)", id, session);
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Member)) {
			return false;
		}
		Member m = (Member) o;
		return new EqualsBuilder()//
				.append(m.id, id)//
				.isEquals();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder()//
				.append(id)//
				.build();
	}

}
