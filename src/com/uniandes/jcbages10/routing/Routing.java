package com.uniandes.jcbages10.routing;

import com.uniandes.jcbages10.tuplespace.ITuple;
import com.uniandes.jcbages10.tuplespace.ITupleSpace;
import com.uniandes.jcbages10.tuplespace.TupleSpace;

import java.util.*;

public class Routing implements IRouting<ITuple> {

    /**
     * Maximum number of devices to remember
     */
    private final static int MAX_NUMBER_OF_HOSTS = 100;

    /**
     * Threshold of time before connecting again in milliseconds
     */
    private final static int RECENT_CONNECTION_THRESHOLD = 30000;

    /**
     * Singleton instance of routing
     */
    private static IRouting<ITuple> instance;

    /**
     * Host ID
     */
    private UUID id;

    /**
     * Dummy object for blocking edit
     */
    private final Object editBlock;

    /**
     * Map from message.id() -> message
     */
    private Map<UUID, IMessage<ITuple>> messages;

    /**
     * Map from message.element() -> message
     */
    private Map<ITuple, IMessage<ITuple>> elementToMessage;

    /**
     * Map from hosts IDs to last time of connection
     */
    private Map<UUID, Long> recentlyConnectedHosts;

    /**
     * Map from last time of connection to list of nodes connected at that time
     */
    private TreeMap<Long, UUID> timeToHosts;

    /**
     * Reference to the TS singleton instance
     */
    private static ITupleSpace tupleSpace = TupleSpace.getInstance();

    /**
     * Constructor for initializing id, maps & dummy objects
     */
    private Routing() {
        // Initialize id
        this.id = UUID.randomUUID();

        // Initialize maps
        this.messages = new HashMap<>();
        this.elementToMessage = new HashMap<>();

        this.recentlyConnectedHosts = new HashMap<>();
        this.timeToHosts = new TreeMap<>();

        // Initialize dummy block object
        this.editBlock = new Object();
    }

    /**
     * Get the singleton instance of Routing
     * @return The singleton instance of routing
     */
    public static synchronized IRouting<ITuple> getInstance() {
        if (instance == null) {
            instance = new Routing();
        }
        return instance;
    }

    /**
     * Return the ID of the host for routing
     * @return ID of host
     */
    @Override
    public UUID id() {
        return this.id;
    }

    /**
     * Add a new message to the maps with the given element
     * @param element The element to add
     */
    @Override
    public void add(ITuple element) {
        synchronized (this.editBlock) {
            IMessage<ITuple> message = new Message<>(element);
            this.messages.put(message.id(), message);
            this.elementToMessage.put(message.element(), message);
        }
    }

    /**
     * Remove a message that contains the given element
     * @param element The element to remove
     */
    @Override
    public void remove(ITuple element) {
        synchronized (this.editBlock) {
            if (this.elementToMessage.containsKey(element)) {
                IMessage<ITuple> message = this.elementToMessage.get(element);
                this.messages.remove(message.id());
                this.elementToMessage.remove(message.element());
            }
        }
    }

    /**
     * Return a list of the messages IDs stored and able to be exchanged,
     * that is, messages with hop count greater than 0
     * @return A list of the messages IDs stored
     */
    @Override
    public List<UUID> messagesIds() {
        List<UUID> result = new ArrayList<>();
        for (IMessage<ITuple> message : this.messages.values()) {
            if (message.hopCount() > 0) {
                result.add(message.id());
            }
        }
        return result;
    }

    /**
     * Returns a list with the messages containing any of the given IDs
     * @param messagesIds The requested messages IDs
     * @return The list with the requested messages
     */
    @Override
    public List<IMessage<ITuple>> sendMessages(List<UUID> messagesIds) {
        List<IMessage<ITuple>> result = new ArrayList<>();
        for (UUID id : messagesIds) {
            synchronized (this.editBlock) {
                if (this.messages.containsKey(id)) {
                    result.add(this.messages.get(id));
                }
            }
        }
        return result;
    }

    /**
     * Returns a list with the messages IDs I don't have but are in the given list
     * @param messagesIds The messages IDs of other node
     * @return The messages IDs I need from that node
     */
    @Override
    public List<UUID> requestMessages(List<UUID> messagesIds) {
        List<UUID> result = new ArrayList<>();
        for (UUID id : messagesIds) {
            if (!this.messages.containsKey(id)) {
                result.add(id);
            }
        }
        return result;
    }

    /**
     * Receive a list of messages adding them to the TS & the maps
     * @param messages The messages to add
     */
    @Override
    public void receiveMessages(List<IMessage<ITuple>> messages) {
        synchronized (this.editBlock) {
            ITuple[] tuples = new ITuple[messages.size()];
            for (int i = 0; i < messages.size(); ++i) {
                IMessage<ITuple> receivedMessage = messages.get(i);
                if (!this.messages.containsKey(receivedMessage.id())) {
                    IMessage<ITuple> message = new Message<>(receivedMessage);
                    this.messages.put(message.id(), message);
                    this.elementToMessage.put(message.element(), message);
                    tuples[i] = message.element();
                }
            }
            tupleSpace.outRouting(tuples);
        }
    }

    /**
     * Determines if a connecting device is allowed to communicate
     * based on the last time they communicate and the communication threshold,
     * if they can communicate then save the information in a map
     * @param hostId The ID of the host trying to communicate
     * @return True or false whether the host can communicate
     */
    @Override
    public synchronized boolean shouldCommunicate(UUID hostId) {
        boolean result;
        long currentTime = System.currentTimeMillis();
        if (!this.recentlyConnectedHosts.containsKey(hostId)) {
            cleanOldestEntryIfNecessary();
            addHostRecentConnection(hostId, currentTime);
            result = true;
        } else {
            long lastTime = this.recentlyConnectedHosts.get(hostId);
            if (currentTime - lastTime > RECENT_CONNECTION_THRESHOLD) {
                addHostRecentConnection(hostId, currentTime);
                result = true;
            } else {
                result = false;
            }
        }
        return result;
    }

    /**
     * Add the given host to the recently connected hosts cache
     * @param hostId The host ID to add
     * @param currentTime The time of connection with the host
     */
    private void addHostRecentConnection(UUID hostId, long currentTime) {
        this.recentlyConnectedHosts.put(hostId, currentTime);
        this.timeToHosts.put(currentTime, hostId);
    }

    /**
     * Remove the oldest entry from the recently connected hosts cache
     * in case it has reached the size limit, oldest in connection time
     */
    private void cleanOldestEntryIfNecessary() {
        if (this.recentlyConnectedHosts.size() == MAX_NUMBER_OF_HOSTS) {
            long oldestTime = this.timeToHosts.firstKey();
            UUID oldestHost = this.timeToHosts.get(oldestTime);

            this.recentlyConnectedHosts.remove(oldestHost);
            this.timeToHosts.remove(oldestTime);
        }
    }

}
