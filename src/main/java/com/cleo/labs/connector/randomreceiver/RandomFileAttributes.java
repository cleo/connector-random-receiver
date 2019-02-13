package com.cleo.labs.connector.randomreceiver;

import java.io.IOException;
import java.nio.file.attribute.DosFileAttributeView;
import java.nio.file.attribute.DosFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import com.cleo.connector.api.property.ConnectorPropertyException;

/**
 * Random file attribute views
 */
public class RandomFileAttributes implements DosFileAttributes, DosFileAttributeView {
    RandomReceiverConnectorConfig config;

    public RandomFileAttributes(RandomReceiverConnectorConfig config) {
        this.config = config;
    }

    @Override
    public FileTime lastModifiedTime() {
        return FileTime.from(new Date().getTime(), TimeUnit.MILLISECONDS);
    }

    @Override
    public FileTime lastAccessTime() {
        return FileTime.from(new Date().getTime(), TimeUnit.MILLISECONDS);
    }

    @Override
    public FileTime creationTime() {
        return FileTime.from(new Date().getTime(), TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean isRegularFile() {
        return true;
    }

    @Override
    public boolean isDirectory() {
        return false;
    }

    @Override
    public boolean isSymbolicLink() {
        return false;
    }

    @Override
    public boolean isOther() {
        return false;
    }

    @Override
    public long size() {
        try {
            return RandomReceiverConnectorClient.parseLength(config.getLength());
        } catch (ConnectorPropertyException e) {
            return 0L;
        }
    }

    @Override
    public Object fileKey() {
        return null;
    }

    @Override
    public void setTimes(FileTime lastModifiedTime, FileTime lastAccessTime, FileTime createTime) throws IOException {
        if (lastModifiedTime != null || lastAccessTime != null || createTime != null) {
            throw new UnsupportedOperationException("setTimes() not supported on Random streams");
        }
    }

    @Override
    public String name() {
        return "random.txt";
    }

    @Override
    public DosFileAttributes readAttributes() throws IOException {
        return this;
    }

    @Override
    public void setReadOnly(boolean value) throws IOException {
        throw new UnsupportedOperationException("setHidden() not supported on Random streams");
    }

    @Override
    public void setHidden(boolean value) throws IOException {
        throw new UnsupportedOperationException("setHidden() not supported on Random streams");
    }

    @Override
    public void setSystem(boolean value) throws IOException {
        throw new UnsupportedOperationException("setSystem() not supported on Random streams");
    }

    @Override
    public void setArchive(boolean value) throws IOException {
        throw new UnsupportedOperationException("setArchive() not supported on Random streams");
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public boolean isHidden() {
        return false;
    }

    @Override
    public boolean isArchive() {
        return false;
    }

    @Override
    public boolean isSystem() {
        return false;
    }

}
