package com.uniandes.jcbages10.tuplespace;

public interface IField<T> {

    boolean isFormal();

    boolean isActual();

    Class<T> type();

    T element();

}
