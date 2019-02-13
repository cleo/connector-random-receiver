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

### The Receiver ###

### Sessions ###

### Files ###

