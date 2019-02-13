# README #

The Cleo Harmony/VLTrader (aka VersaLex) Connector Shell API allows new connector
technologies to be plugged in to be configured and used by the administrator.

This sample project extends the `random` connector example to illustrate the
Connector Shell API's _Receiver_ capability, which allows a connector to generate
unsolicited event-driven files.
While in a real-world use case, this would likely be the result of an event
being produced by the connected system, this sample uses a simple Timer to
produce files named `random-yyyyMMdd-HHmmss.SSS` with random content at a fixed interval.

## TL;DR ##

The POM for this project creates a ZIP archive intended to be expanded from
the Harmony/VLTrader installation directory (`$CLEOHOME` below).

```
git clone git@github.com:cleo/connector-random-receiver.git
mvn clean package
cp target/random-receiver-5.6-SNAPSHOT-distribution.zip $CLEOHOME
cd $CLEOHOME
unzip -o random-receiver-5.6-SNAPSHOT-distribution.zip
./Harmonyd stop
./Harmonyd start
```

When Harmony/VLTrader restarts, you will see a new `Template` in the host tree
under `Connections` > `Generic` > `Generic RandomReceiver`.  Select `Clone and Activate`
and a new `RandomReceiver` connection (host) will appear on the `Active` tab.

Change the `Interval` setting to `60` and Apply.  The receiver will start and will produce
a random file in the inbox once per minute.
Alternatively, enter the name of another host as the `Receive To` and the random files
will be sent to the designated host.

## Connector Shell Receiver Concepts ##

Like a Connector Shell Client (command processor), a Connector Shell Receiver
starts with a [schema](#the-schema) class, which defines the properties required to configure
connector instances and designates the class the implements the [receiver](#the-receiver).

> Note that a connector may implement Client or Receiver behaviors, or both
> (or some additional behaviors not documented in this tutorial).

The receiver class implements a simple life cycle using the `enable(boolean)` method.
When enabled (and properly configured), the receiver class is responsible for
establishing a connection to (or listening for a connection from) the resource it
is managing.

When it has content to be received into Harmony/VLTrader, it establishes a
[session](#sessions).  Each session can be used to receive one or more
[files](#files) before it is ended.

### The Schema ###

Use the schema class to identify configuration properties, as for the `@Client` sample,
but identify the connector as a Receiver using `@Receiver`.

```java
import com.cleo.connector.api.ConnectorConfig;
import com.cleo.connector.api.annotations.Connector;
import com.cleo.connector.api.annotations.Property;
import com.cleo.connector.api.annotations.Receiver;

@Connector(scheme = "RandomReceiver", description = "Random Content Streams")
@Receiver(RandomReceiverConnectorReceiver.class)
public class RandomReceiverConnectorSchema extends ConnectorConfig {
    @Property...
```

### The Receiver ###

The Receiver class, identified in the schema class with `@Receiver`, must extend
the `ConnectorReceiver` base class and implement the `enable` method.

```java
import com.cleo.connector.api.ConnectorException;
import com.cleo.connector.api.ConnectorReceiver;

public class RandomReceiverConnectorReceiver extends ConnectorReceiver {

    public RandomReceiverConnectorReceiver(RandomReceiverConnectorSchema schema) {
        ...
    }

    @Override
    public boolean enable(boolean enable) throws ConnectorException {
        if (enable) {
            // start the receiver
        } else {
            //stop the receiver
        }
    }
```

Whenever the enablement state of the connection changes, the `enable` method will be called.
The Random Receiver sample also shows how the configuration values may change while the
connection is enabled without intervening calls to `enable`.
The Random Receiver has a configured _interval_, which if 0 suspends the reception of files,
yet if adjusted will resume with an adjusted interval.
So `enable(true)` starts a `Timer`, but if the _interval_ is 0, a `CheckTask` is scheduled,
while a positive _interval_ schedules the `ReceiveTask`.

```java
static private final long NO_DELAY = 0L;
static private final long SECONDS = 1000L;
static private final long CHECK = 1;

@Override
public boolean enable(boolean enable) throws ConnectorException {
    if (enable && timer == null) {
        start(config.getInterval());
    } else if (!enable && timer != null) {
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
        timer.schedule(new CheckTask(), NO_DELAY, CHECK * SECONDS);
    } else {
        timer.schedule(new ReceiveTask(), NO_DELAY, interval * SECONDS);
    }
}
```

The `CheckTask` wakes up every second to check for configuration changes, switching
to the `ReceiveTask` when the _interval_ is set.

```java
public class CheckTask extends TimerTask {
    @Override
    public void run() {
        try {
            long interval = config.getInterval();
            if (interval > 0) {
                this.cancel();
                start(interval);
            }
        } catch (ConnectorPropertyException e) {
            // just ignore it
        }
    }
}
```

The `ReceiveTask` illustrates the business end of actually generating a random
content stream and "receiving" it as a [file](#files) over a [session](#sessions) into Harmony/VLTrader.

### Sessions ###

Receiving files happens within a `ReceiverSession`.  To obtain a session, use:

```java
getReceiverSessionFactory().newSession(receiver, targetType, target, metadata);
```

Each session has a `TargetType` that
controls how the received file is to be processed:

* `TargetType.Inbox` &mdash; the received file is stored in the connection inbox in the target subfolder (may be `null` for storage in the inbox directly)
* `TargetType.HostConnectionAlias` &mdash; the received file is streamed through to the connection with the specified target alias (may not be `null`)
* `TargetType.HostConnectionUid` &mdash; the received file is streamed through to the connection with the specified target UID (may not be `null`)

Additional arbitrary session metadata `Map<String,Object>`, which may be `null`, may also be passed to `newSession`.

To pass files through the session, use:

```java
session.receive(IConnectorReceive[] files);
```

and finally call `session.end()`.

To pull it all together (short of the details around [files](#files)):

```java
// receive a file
RandomConnectorReceive file = ...
ReceiverSession session = null;
try {
    if (Strings.isNullOrEmpty(config.getRecceiveTo())) {
        session = getReceiverSessionFactory().newSession(receiver, TargetType.Inbox, null, null);
    } else {
        session = getReceiverSessionFactory().newSession(receiver, TargetType.HostConnectionAlias, config.getRecceiveTo(), null);
    }
    session.receive(new IConnectorReceive[]{file});
} catch (ConnectorException e) {
    logger.logError("error receiving file "+file.getName());
    logger.logThrowable(e);
} finally {
    if (session != null) {
        try {
            session.end();
        } catch (ConnectorException e) {
            logger.logError("error ending session");
            logger.logThrowable(e);
        }
    }
}
```

### Files ###

In a Connector Shell `@Client`, a retrieved file, its content and attributes are represented
in an `Entry` object (in the result from a `DIR` command), a content stream (in the `transfer` method called during the `GET` command), and as a `BasicFileAttributeView` returned from the `ATTR` command.

In a Connector Shell `@Receiver` the file content, attributes, and metadata are consolidated
into a single object extending the `IConnectorReceive` abstract base class:

```java
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
```

An implementation of `getStream` is required.  All other methods have acceptable
default implementations in the base class.  Additional methods not implemented in
the Random Receiver sample include:

* `String getPath()` &mdash; the file path
* `String getFileid()` &mdash; the file unique ID
* `void setTransferId(String transferId)` &mash; set the transfer ID
* `String getTransferId()` &mdash; the transfer ID
* `IConnectorFile getReceivedboxCopy()` &mdash; a copy of the received file


