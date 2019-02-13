package com.cleo.labs.connector.randomreceiver;

import static com.cleo.connector.api.command.ConnectorCommandName.ATTR;
import static com.cleo.connector.api.command.ConnectorCommandName.DELETE;
import static com.cleo.connector.api.command.ConnectorCommandName.GET;
import static com.cleo.connector.api.command.ConnectorCommandName.PUT;
import static com.cleo.connector.api.command.ConnectorCommandOption.Delete;
import static com.cleo.connector.api.command.ConnectorCommandOption.Unique;

import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributeView;

import com.cleo.connector.api.ConnectorClient;
import com.cleo.connector.api.ConnectorException;
import com.cleo.connector.api.annotations.Command;
import com.cleo.connector.api.command.ConnectorCommandResult;
import com.cleo.connector.api.command.GetCommand;
import com.cleo.connector.api.command.OtherCommand;
import com.cleo.connector.api.command.PutCommand;
import com.cleo.connector.api.interfaces.IConnectorIncoming;
import com.cleo.connector.api.interfaces.IConnectorOutgoing;
import com.google.common.base.Strings;
import com.google.common.io.ByteStreams;

public class RandomReceiverConnectorClient extends ConnectorClient {
    private RandomReceiverConnectorConfig config;

    public RandomReceiverConnectorClient(RandomReceiverConnectorSchema schema) {
        this.config = new RandomReceiverConnectorConfig(this, schema);
    }

    @Command(name = PUT, options = { Delete, Unique })
    public ConnectorCommandResult put(PutCommand put) throws ConnectorException, IOException {
        IConnectorOutgoing source = put.getSource();
        String destination = put.getDestination().getPath();

        logger.debug(String.format("PUT local '%s' to remote '%s'", source.getPath(), destination));

        if (config.getMatch()) {
            transfer(source.getStream(), new RandomOutputStream(config.getSeed(), parseLength(config.getLength())), false);
        } else {
            transfer(source.getStream(), ByteStreams.nullOutputStream(), false);
        }
        return new ConnectorCommandResult(ConnectorCommandResult.Status.Success);
    }

    @Command(name = GET)
    public ConnectorCommandResult get(GetCommand get) throws ConnectorException, IOException {
        String source = get.getSource().getPath();
        IConnectorIncoming destination = get.getDestination();

        logger.debug(String.format("GET remote '%s' to local '%s'", source, destination.getPath()));

        transfer(new RandomInputStream(config.getSeed(), parseLength(config.getLength())), destination.getStream(), true);
        return new ConnectorCommandResult(ConnectorCommandResult.Status.Success);
    }

    @Command(name = DELETE)
    public ConnectorCommandResult delete(OtherCommand delete) throws ConnectorException, IOException {
        String source = delete.getSource();
        logger.debug(String.format("DELETE '%s'", source));

        /*
        throw new ConnectorException(String.format("'%s' does not exist or is not accessible", source),
                ConnectorException.Category.fileNonExistentOrNoAccess);
        */

        return new ConnectorCommandResult(ConnectorCommandResult.Status.Success);
    }

    /**
     * Get the file attribute view associated with a file path
     * 
     * @param path the file path
     * @return the file attributes
     * @throws com.cleo.connector.api.ConnectorException
     * @throws java.io.IOException
     */
    @Command(name = ATTR)
    public BasicFileAttributeView getAttributes(String path) throws ConnectorException, IOException {
        return new RandomFileAttributes(config);
    }

    /* Parses an optionally suffixed length:
     * <ul>
     * <li><b>nnnK</b> nnn KB (technically "kibibytes", * 1024)</li>
     * <li><b>nnnM</b> nnn MB ("mebibytes", * 1024^2)</li>
     * <li><b>nnnG</b> nnn GB ("gibibytes", * 1024^3)</li>
     * <li><b>nnnT</b> nnn TB ("tebibytes", * 1024^4)</li>
     * </ul>
     * Note that suffixes may be upper or lower case.  A trailing "b"
     * (e.g. kb, mb, ...) is tolerated but not required.
     * @param length the string to parse
     * @return the parsed long
     * @throws {@link NumberFormatException}
     * @see {@link Long#parseLong(String)}
     */
    public static long parseLength(String length) {
        if (!Strings.isNullOrEmpty(length)) {
            long multiplier = 1L;
            int  check = length.length()-1;
            if (check>=0) {
                char suffix = length.charAt(check);
                if ((suffix=='b' || suffix=='B') && check>0) {
                    check--;
                    suffix = length.charAt(check);
                }
                switch (suffix) {
                case 'k': case 'K': multiplier =                   1024L; break;
                case 'm': case 'M': multiplier =             1024L*1024L; break;
                case 'g': case 'G': multiplier =       1024L*1024L*1024L; break;
                case 't': case 'T': multiplier = 1024L*1024L*1024L*1024L; break;
                default:
                }
                if (multiplier != 1) {
                    length = length.substring(0, check);
                }
            }
            return Long.parseLong(length)*multiplier;
        }
        return 0L;
    }
}
