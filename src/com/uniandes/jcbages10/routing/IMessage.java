package com.uniandes.jcbages10.routing;

import java.util.UUID;

public interface IMessage<T> {

    UUID id();

    int hopCount();

    T element();

}
