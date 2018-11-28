package com.uniandes.jcbages10.routing;

import java.util.List;
import java.util.UUID;

public interface IRouting<T> {

    UUID id();

    void add(T element);

    void remove(T element);

    List<UUID> messagesIds();

    List<IMessage<T>> sendMessages(List<UUID> messagesIds);

    List<UUID> requestMessages(List<UUID> messagesIds);

    void receiveMessages(List<IMessage<T>> messages);

    boolean shouldCommunicate(UUID hostId);

}
