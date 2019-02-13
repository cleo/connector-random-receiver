package com.cleo.labs.connector.randomreceiver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Date;

import org.junit.Test;

import com.google.common.io.ByteStreams;

public class TestRandomReceiverConnectorClient {

    public static final long KB = 1024L;
    public static final long MB = 1024L*KB;
    public static final long GB = 1024L*MB;
    public static final long TB = 1024L*GB;

    @Test
    public final void test500M() {
        long seed = new Date().getTime();
        long length = RandomReceiverConnectorClient.parseLength("500K");
        try (RandomInputStream in = new RandomInputStream(seed, length);
             RandomOutputStream out = new RandomOutputStream(seed, length)) {
            assertEquals(500*KB, ByteStreams.copy(in, out));
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public final void test500Mzero() {
        long seed = 0;
        long length = RandomReceiverConnectorClient.parseLength("500M");
        try (RandomInputStream in = new RandomInputStream(seed, length);
             RandomOutputStream out = new RandomOutputStream(seed, length)) {
            assertEquals(500*MB, ByteStreams.copy(in, out));
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public final void testFailOnExtraWrite() {
        long seed = new Date().getTime();
        long length = 400;
        try (RandomInputStream in = new RandomInputStream(seed, length);
             RandomOutputStream out = new RandomOutputStream(seed, length)) {
            out.write(in.read()); // advance one byte
            ByteStreams.copy(in, out); // copy the rest
            try {
                out.write('a');
                fail("exception expected on extra write");
            } catch (IOException e) {
                // expected
            }
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public final void testParseLength() {
        assertEquals(1024, RandomReceiverConnectorClient.parseLength("1k"));
        assertEquals(1024, RandomReceiverConnectorClient.parseLength("1Kb"));
        assertEquals(MB, RandomReceiverConnectorClient.parseLength("1m"));
        assertEquals(5*TB, RandomReceiverConnectorClient.parseLength("5tb"));
    }
}
