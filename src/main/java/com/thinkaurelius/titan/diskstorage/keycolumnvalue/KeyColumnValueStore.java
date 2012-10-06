package com.thinkaurelius.titan.diskstorage.keycolumnvalue;

import com.thinkaurelius.titan.diskstorage.StorageException;

import java.nio.ByteBuffer;
import java.util.List;

public interface KeyColumnValueStore {

    /**
     * Returns true if the specified key exists in the store, i.e. there is at least one column-value
     * pair for the key.
     *
     * @param key Key
     * @param txh Transaction
     * @return TRUE, if key has at least one column-value pair, else FALSE
     */
    public boolean containsKey(ByteBuffer key, StoreTransactionHandle txh) throws StorageException;

    /**
     * Retrieves the list of entries (i.e. column-value pairs) for a specified key which
     * lie between the specified start and end columns.
     * The start and end columns are considered to be inclusive and exclusive, respectively.
     *
     * Only retrieves a maximum number of entries as specified by the limit.
     *
     * @param key Key
     * @param columnStart Tail Column (inclusive)
     * @param columnEnd Head Column (exclusive)
     * @param limit Maximum number of entries to retrieve
     * @param txh Transaction
     * @throws StorageException when columnEnd < columnStart as determined in
     *         {@link com.thinkaurelius.titan.diskstorage.util.ByteBufferUtil#isSmallerThan(ByteBuffer,ByteBuffer)}
     * @return List of entries up to a maximum of "limit" entries
     */
    public List<Entry> getSlice(ByteBuffer key, ByteBuffer columnStart, ByteBuffer columnEnd, int limit, StoreTransactionHandle txh) throws StorageException;


    /**
     * Retrieves the list of entries (i.e. column-value pairs) for a specified key which
     * lie between the specified start and end columns.
     * The start and end columns are considered to be inclusive and exclusive, respectively.
     *
     * Retrieves all entries.
     *
     * @param key Key
     * @param columnStart Tail Column (inclusive)
     * @param columnEnd Head Column (exclusive)
     * @param txh Transaction
     * @throws StorageException when columnEnd < columnStart as determined in
     *         {@link com.thinkaurelius.titan.diskstorage.util.ByteBufferUtil#isSmallerThan(ByteBuffer,ByteBuffer)}
     * @return List of entries
     */
    public List<Entry> getSlice(ByteBuffer key, ByteBuffer columnStart, ByteBuffer columnEnd, StoreTransactionHandle txh) throws StorageException;

    /**
     * Retrieves the value for the specified column and key under the given transaction
     * from the store if such exists, otherwise NULL
     *
     * @param key Key
     * @param column Column
     * @param txh Transaction
     * @return Value for key and column or NULL
     */
    public ByteBuffer get(ByteBuffer key, ByteBuffer column, StoreTransactionHandle txh) throws StorageException;

    /**
     * Returns true if the specified key-column pair exists in the store.
     *
     * @param key Key
     * @param column Column
     * @param txh Transaction
     * @return TRUE, if key has at least one column-value pair, else FALSE
     */
    public boolean containsKeyColumn(ByteBuffer key, ByteBuffer column, StoreTransactionHandle txh) throws StorageException;

    /**
     * Applies the specified insertion and deletion mutations to the provided key.
     * Both, the list of additions or deletions, may be empty or NULL if there is nothing to be added and/or deleted.
     *
     * @param key Key
     * @param additions List of entries (column + value) to be added
     * @param deletions List of columns to be removed
     * @param txh Transaction under which to execute the operation
     */
    public void mutate(ByteBuffer key, List<Entry> additions, List<ByteBuffer> deletions, StoreTransactionHandle txh) throws StorageException;

    /**
     * Acquires a lock for the key-column pair which ensures that nobody else can take a lock on that
     * respective entry for the duration of this lock (but somebody could potentially still overwrite
     * the key-value entry without taking a lock).
     * The expectedValue defines the value expected to match the value at the time the lock is acquired (or null if it is expected
     * that the key-column pair does not exist).
     *
     * If this method is called multiple times with the same key-column pair in the same transaction, all but the first invocation are ignored.
     *
     * The lock has to be released when the transaction closes (commits or aborts).
     *
     * @param key Key on which to lock
     * @param column Column the column on which to lock
     * @param expectedValue The expected value for the specified key-column pair on which to lock. Null if it is expected that the pair does not exist
     * @param txh Transaction
     */
    public void acquireLock(ByteBuffer key, ByteBuffer column, ByteBuffer expectedValue, StoreTransactionHandle txh) throws StorageException;


    /**
     * Returns an iterator over all keys in this store. The keys may be
     * ordered but not necessarily.
     *
     * @return An iterator over all keys in this store.
     */
    public RecordIterator<ByteBuffer> getKeys(StoreTransactionHandle txh) throws StorageException;

    /**
     * Returns the name of this store. Each store has a unique name which is used to open it.
     *
     * @see KeyColumnValueStoreManager#openDatabase(String)
     * @return
     */
    public String getName();
    
    /**
     * Closes this store
     *
     * @throws StorageException
     */
    public void close() throws StorageException;


}
