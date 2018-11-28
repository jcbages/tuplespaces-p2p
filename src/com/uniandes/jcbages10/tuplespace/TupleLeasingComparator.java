package com.uniandes.jcbages10.tuplespace;

import java.util.Comparator;

public class TupleLeasingComparator implements Comparator<ITuple> {

    @Override
    public int compare(ITuple tuple1, ITuple tuple2) {
        return Long.compare(tuple1.leasing(), tuple2.leasing());
    }

}
