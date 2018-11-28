package com.uniandes.jcbages10.routing;

import java.util.UUID;

public class Message<T> implements IMessage<T> {

    /**
     * The default initial hop count
     */
    private final static int INITIAL_HOP_COUNT = 5;

    /**
     * The unique ID of the message
     */
    private UUID id;

    /**
     * The element contained by the message
     */
    private T element;

    /**
     * The hop count of the message
     */
    private int hopCount;

    /**
     * Constructor for initializing a random id,
     * the message element with the given one, and
     * the hop count with the default initial value
     * @param element The element of the message
     */
    public Message(T element) {
        this.id = UUID.randomUUID();
        this.element = element;
        this.hopCount = INITIAL_HOP_COUNT;
    }

    /**
     * Constructor for initializing a message with another given one,
     * this constructor is for received messages so it reduces hop count by one
     * @param message The received message to copy
     */
    public Message(IMessage<T> message) {
        this.id = message.id();
        this.element = message.element();
        this.hopCount = Math.max(0, message.hopCount() - 1);
    }

    /**
     * Get the message ID
     * @return The message ID
     */
    @Override
    public UUID id() {
        return this.id;
    }

    /**
     * Get the message hop count
     * @return The message hop count
     */
    @Override
    public int hopCount() {
        return this.hopCount;
    }

    /**
     * Get the message element
     * @return The message element
     */
    @Override
    public T element() {
        return this.element;
    }

}
