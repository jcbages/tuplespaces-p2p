package com.uniandes.jcbages10.tuplespace;

import java.util.Optional;

public interface ITuple {

    Optional<ITuple> match(ITuple tuple);

    IField get(int position);

    int length();

    long leasing();

}
