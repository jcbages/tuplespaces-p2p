package com.uniandes.jcbages10.tuplespace;

import java.util.concurrent.Future;

public interface ITupleSpace {

    void out(ITuple tuple);

    void outMany(ITuple... tuple);

    void outRouting(ITuple... tuples);

    Future<ITuple> in(ITuple tuple);

    Future<ITuple> read(ITuple tuple);

}
