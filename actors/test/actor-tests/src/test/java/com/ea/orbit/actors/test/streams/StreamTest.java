package com.ea.orbit.actors.test.streams;


import com.ea.orbit.actors.Stage;
import com.ea.orbit.actors.streams.AsyncStream;
import com.ea.orbit.actors.streams.StreamSubscriptionHandle;
import com.ea.orbit.actors.test.ActorBaseTest;
import com.ea.orbit.concurrent.Task;

import org.junit.Test;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class StreamTest extends ActorBaseTest
{
    @SuppressWarnings("Duplicates")
    @Test(timeout = 30_000L)
    public void test()
    {
        createStage();
        CompletableFuture<String> push = new Task<>();
        AsyncStream<String> test = AsyncStream.getStream(String.class, "test");
        test.subscribe((d,t) -> {
            push.complete(d);
            return Task.done();
        }).join();
        test.publish("hello");
        assertEquals("hello", push.join());
        dumpMessages();
    }

    @SuppressWarnings("Duplicates")
    @Test(timeout = 30_000L)
    public void testStreamPersistence() throws InterruptedException
    {
        final Stage stage1 = createStage();
        // forces the stream to be created in the fist stage.
        AsyncStream.getStream(String.class, "test")
                .publish("data").join();

        // create a second stage from which the stream is going to be used
        createStage();
        AsyncStream<String> test = AsyncStream.getStream(String.class, "test");
        BlockingQueue queue = new LinkedBlockingQueue<>();
        test.subscribe((msg,t) -> {
            queue.add(msg);
            return Task.done();
        }).join();
        test.publish("hello");
        assertEquals("hello", queue.poll(10, TimeUnit.SECONDS));

        // Stopping the first stage, the stream should migrate on the next post.
        stage1.stop().join();

        test.publish("hello2");
        assertEquals("hello2", queue.poll(5, TimeUnit.SECONDS));

        dumpMessages();
    }


    @Test(timeout = 30_000L)
    public void test2Stages()
    {
        CompletableFuture<String> push1 = new Task<>();
        final Stage stage1 = createStage();
        {
            AsyncStream<String> test = AsyncStream.getStream(String.class, "test");
            test.subscribe((d,t) -> {
                push1.complete(d);
                return Task.done();
            }).join();
        }

        final Stage stage2 = createStage();
        {
            stage2.bind();
            AsyncStream<String> test2 = AsyncStream.getStream(String.class, "test");
            test2.publish("hello");
        }
        assertEquals("hello", push1.join());
        dumpMessages();
    }


    @Test(timeout = 30_000L)
    public void test3Stages()
    {
        CompletableFuture<String> push1 = new Task<>();
        final Stage stage1 = createStage();
        final Stage stage2 = createStage();
        final Stage stage3 = createStage();
        {
            AsyncStream<String> test = AsyncStream.getStream(String.class, "test");
            test.subscribe((d,t) -> {
                push1.complete(d);
                return Task.done();
            }).join();
        }
        CompletableFuture<String> push2 = new Task<>();
        {
            AsyncStream<String> test2 = AsyncStream.getStream(String.class, "test");
            test2.subscribe((d,t) -> {
                push2.complete(d);
                return Task.done();
            }).join();
        }

        {
            stage3.bind();
            AsyncStream<String> test3 = AsyncStream.getStream(String.class, "test");
            test3.publish("hello");
        }
        assertEquals("hello", push1.join());
        assertEquals("hello", push2.join());
        dumpMessages();
    }


    @Test(timeout = 30_000L)
    public void test3StagesAndGc()
    {
        CompletableFuture<String> push1 = new Task<>();
        final Stage stage1 = createStage();
        {
            AsyncStream<String> test = AsyncStream.getStream(String.class, "test");
            test.subscribe((d,t) -> {
                push1.complete(d);
                return Task.done();
            }).join();
        }
        CompletableFuture<String> push2 = new Task<>();
        final Stage stage2 = createStage();
        {
            AsyncStream<String> test2 = AsyncStream.getStream(String.class, "test");
            test2.subscribe((d,t) -> {
                push2.complete(d);
                return Task.done();
            }).join();
        }

        System.gc();

        final Stage stage3 = createStage();
        {
            stage3.bind();
            AsyncStream<String> test3 = AsyncStream.getStream(String.class, "test");
            test3.publish("hello");
        }
        assertEquals("hello", push1.join());
        assertEquals("hello", push2.join());
        dumpMessages();
    }


    @Test(timeout = 30_000L)
    public void testUnSubscription()
    {
        CompletableFuture<String> push1 = new Task<>();
        final Stage stage1 = createStage();
        final AsyncStream<String> test = AsyncStream.getStream(String.class, "test");
        final StreamSubscriptionHandle<String> handle1 = test.subscribe((d,t) -> {
            push1.complete(d);
            return Task.done();
        }).join();

        final CompletableFuture<String> push2 = new Task<>();
        final Stage stage2 = createStage();
        System.gc();
        final Stage stage3 = createStage();
        stage3.bind();
        final AsyncStream<String> test3 = AsyncStream.getStream(String.class, "test");

        stage2.bind();
        final AsyncStream<String> test2 = AsyncStream.getStream(String.class, "test");
        test2.subscribe((d,t) -> {
            push2.complete(d);
            return Task.done();
        }).join();
        test.unsubscribe(handle1).join();

        test3.publish("hello2").join();

        assertEquals("hello2", push2.join());
        assertFalse(push1.isDone());
        dumpMessages();
    }
}
