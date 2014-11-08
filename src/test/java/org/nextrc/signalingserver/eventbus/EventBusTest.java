package org.nextrc.signalingserver.eventbus;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.nextrtc.signalingserver.api.annotation.NextRTCEvents.SESSION_CLOSED;
import static org.nextrtc.signalingserver.api.annotation.NextRTCEvents.SESSION_STARTED;
import lombok.Data;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nextrtc.signalingserver.api.NextRTCEvent;
import org.nextrtc.signalingserver.api.NextRTCHandler;
import org.nextrtc.signalingserver.api.annotation.NextRTCEventListener;
import org.nextrtc.signalingserver.api.annotation.NextRTCEvents;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { TestBus.class, T1.class, T2.class, T3.class })
public class EventBusTest {
	
	@Autowired
	@Qualifier("nextRTCEventBus")
	private EventBus eventBus;

	@Autowired
	@Qualifier("t1")
	private T1 t1;

	@Autowired
	@Qualifier("t2")
	private T2 t2;

	@Autowired
	@Qualifier("t3")
	private T3 t3;

	@Test
	public void should_register_listener_with_NextRTCEventListener_annotation() throws InterruptedException {
		// given
		Object object = new Object();

		// when
		eventBus.post(object);

		// then
		assertThat(t1.getO(), is(object));
	}

	@Test
	public void should_call_handleEvent_method() throws Exception {
		// given
		NextRTCEvent event = event(SESSION_STARTED);
		NextRTCEvent notValidEvent = event(SESSION_CLOSED);

		// when
		eventBus.post(event);
		eventBus.post(notValidEvent);

		// then
		assertThat(t2.getEvent(), is(event));
		assertThat(t3.getEvent(), nullValue());
	}

	private NextRTCEvent event(final NextRTCEvents event) {
		return new NextRTCEvent() {

			@Override
			public NextRTCEvents getType() {
				return event;
			}
		};
	}

	@After
	public void resetClass() {
		t1.setO(null);
		t2.setEvent(null);
		t3.setEvent(null);
	}
}

@Data
@Component("t1")
@NextRTCEventListener
class T1 {

	private Object o;

	@Subscribe
	public void callMe(Object o) {
		this.o = o;
	}
}

@Data
@Component("t2")
@NextRTCEventListener(SESSION_STARTED)
class T2 implements NextRTCHandler {

	private NextRTCEvent event;

	@Override
	public void handleEvent(NextRTCEvent event) {
		this.event = event;
	}
}

@Data
@Component("t3")
@NextRTCEventListener
class T3 implements NextRTCHandler {

	private NextRTCEvent event;

	@Override
	public void handleEvent(NextRTCEvent event) {
		this.event = event;
	}
}