package com.cleo.labs.connector.randomreceiver;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.cleo.connector.api.receiver.IConnectorReceive;
import com.google.common.net.MediaType;

public class RandomConnectorReceive extends IConnectorReceive {
    private long seed;
    private long length;
    private RandomInputStream stream;
    private Date now;

    public RandomConnectorReceive(long seed, long length) {
        this.seed = seed;
        this.length = length;
        this.stream = new RandomInputStream(seed, length);
        this.now = new Date();
    }

    @Override
    public InputStream getStream() {
        return stream;
    }

    private static final SimpleDateFormat yyyymmddhhmmss = new SimpleDateFormat("yyyyMMdd-HHmmss.SSS");

    @Override
    public String getName() {
        return "random."+yyyymmddhhmmss.format(now);
    }

    @Override
    public Long getLength() {
        return length;
    }

    @Override
    public String getContentType() {
        return MediaType.OCTET_STREAM.toString();
    }

    @Override
    public Optional<Map<String, Object>> getMetadata() {
        Map<String,Object> result = new HashMap<>();
        result.put("seed", seed);
        result.put("length", length);
        result.put("content-type", getContentType());
        return Optional.of(result);
    }
}
