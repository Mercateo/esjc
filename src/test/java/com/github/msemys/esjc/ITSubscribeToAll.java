package com.github.msemys.esjc;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertTrue;

public class ITSubscribeToAll extends AbstractEventStoreTest {

    public ITSubscribeToAll(EventStore eventstore) {
        super(eventstore);
    }

    @Test
    public void allowsMultipleSubscriptions() throws InterruptedException {
        final String stream = generateStreamName();

        CountDownLatch eventSignal = new CountDownLatch(2);

        eventstore.subscribeToAll(false, (s, e) -> eventSignal.countDown()).join();
        eventstore.subscribeToAll(false, (s, e) -> eventSignal.countDown()).join();
        eventstore.appendToStream(stream, ExpectedVersion.NO_STREAM, newTestEvent()).join();

        assertTrue("onEvent timeout", eventSignal.await(10, SECONDS));
    }

    @Test
    public void catchesDeletedEventsAsWell() throws InterruptedException {
        final String stream = generateStreamName();

        CountDownLatch eventSignal = new CountDownLatch(1);

        eventstore.subscribeToAll(false, (s, e) -> eventSignal.countDown()).join();
        eventstore.deleteStream(stream, ExpectedVersion.NO_STREAM, true).join();

        assertTrue("onEvent timeout", eventSignal.await(10, SECONDS));
    }

}
