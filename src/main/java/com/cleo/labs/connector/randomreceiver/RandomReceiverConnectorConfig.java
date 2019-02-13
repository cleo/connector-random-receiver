package com.cleo.labs.connector.randomreceiver;

import com.cleo.connector.api.interfaces.ConnectorBase;
import com.cleo.connector.api.property.ConnectorPropertyException;

public class RandomReceiverConnectorConfig {
    private ConnectorBase client;
    private RandomReceiverConnectorSchema schema;

    public RandomReceiverConnectorConfig(ConnectorBase client, RandomReceiverConnectorSchema schema) {
        this.client = client;
        this.schema = schema;
    }

    public long getSeed() throws ConnectorPropertyException {
        return schema.seed.getValue(client);
    }

    public String getLength() throws ConnectorPropertyException {
        return schema.length.getValue(client);
    }

    public boolean getMatch() throws ConnectorPropertyException {
        return schema.match.getValue(client);
    }

    public long getInterval() throws ConnectorPropertyException {
        return schema.interval.getValue(client);
    }

    public String getRecceiveTo() throws ConnectorPropertyException {
        return schema.receiveto.getValue(client);
    }
}
