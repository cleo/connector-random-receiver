package com.cleo.labs.connector.randomreceiver;

import java.util.Timer;
import java.util.TimerTask;

import com.cleo.connector.api.ConnectorException;
import com.cleo.connector.api.ConnectorReceiver;
import com.cleo.connector.api.property.ConnectorPropertyException;
import com.cleo.connector.api.receiver.IConnectorReceive;
import com.cleo.connector.shell.interfaces.ReceiverSession;
import com.cleo.connector.shell.interfaces.ReceiverSessionFactory.TargetType;
import com.google.common.base.Strings;

public class RandomReceiverConnectorReceiver extends ConnectorReceiver {
    private RandomReceiverConnectorConfig config;
    private Timer timer;
    private ConnectorReceiver receiver;

    public RandomReceiverConnectorReceiver(RandomReceiverConnectorSchema schema) {
        this.config = new RandomReceiverConnectorConfig(this, schema);
        this.timer = null;
        this.receiver = this;
    }

    static private final long NO_DELAY = 0L;
    static private final long SECONDS = 1000L;
    static private final long CHECK = 1;

    @Override
    public boolean enable(boolean enable) throws ConnectorException {
        if (enable && timer == null) {
            start(config.getInterval());
        } else if (!enable && timer != null) {
            logger.debug("random file schedule disabled");
            timer.cancel();
            timer = null;
        } else {
            // already in the correct state
        }
        return enable;
    }

    public synchronized void start(long interval) throws ConnectorPropertyException {
        timer = new Timer();
        if (config.getInterval() <= 0) {
            logger.debug(String.format("non-positive interval %d: waiting", interval));
            timer.schedule(new CheckTask(), NO_DELAY, CHECK * SECONDS);
        } else {
            logger.debug(String.format("scheduling random file every %d seconds", interval));
            timer.schedule(new ReceiveTask(), NO_DELAY, interval * SECONDS);
        }
    }

    public class ReceiveTask extends TimerTask {
        @Override
        public void run() {
            try {
                long interval = config.getInterval();
                if (interval <= 0) {
                    this.cancel();
                    logger.debug("shifting to waiting");
                    start(interval);
                } else {
                    // receive a file
                    RandomConnectorReceive file = new RandomConnectorReceive(config.getSeed(),
                            RandomReceiverConnectorClient.parseLength(config.getLength()));
                    logger.logError("receiving file "+file.getName());
                    try {
                        ReceiverSession session;
                        if (Strings.isNullOrEmpty(config.getRecceiveTo())) {
                            session = getReceiverSessionFactory().newSession(receiver, TargetType.Inbox, null, null);
                        } else {
                            session = getReceiverSessionFactory().newSession(receiver, TargetType.HostConnectionAlias, config.getRecceiveTo(),null);
                        }
                        session.receive(new IConnectorReceive[]{file});
                        session.end();
                    } catch (ConnectorException e) {
                        logger.logError("error receiving file "+file.getName());
                        logger.logThrowable(e);
                    }
                }
            } catch (ConnectorPropertyException e) {
                // just ignore it
            }
        }
    }

    public class CheckTask extends TimerTask {
        @Override
        public void run() {
            try {
                long interval = config.getInterval();
                if (interval > 0) {
                    this.cancel();
                    logger.debug("the wait is over");
                    start(interval);
                }
            } catch (ConnectorPropertyException e) {
                // just ignore it
            }
        }
    }
}
