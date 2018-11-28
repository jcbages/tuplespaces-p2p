package com.uniandes.jcbages10;

import com.uniandes.jcbages10.tuplespace.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

public class Main {

    public static void main(String... args) {
        Main main = new Main();
        main.run();
    }

    private void run() {
        ITupleSpace TS = TupleSpace.getInstance();

        List<Future<ITuple>> readCalls = new ArrayList<>();

        // insert read tuples requests
        // (10, *: str)
        System.out.println("INSERTING READ TUPLES");
        for (int i = 0; i < 50; ++i) {
            Future<ITuple> tuple = TS.in(new Tuple(
                new Field<>(Integer.class, 10),
                new Field<>(String.class)
            ));
            readCalls.add(tuple);
        }

        // validate status
        System.out.println("PRE VALIDATING READ TUPLES");
        for (int i = 0; i < 50; ++i) {
            Future<ITuple> tuple = readCalls.get(i);
            if (tuple.isDone()) System.out.println("PREV: UNEXPECTED, TUPLE SHOULD NOT BE DONE YET");
        }


        // insert tuples
        // (*: int, "hello my friend")
        System.out.println("INSERTING TUPLES");
        for (int i = 0; i < 50; ++i) {
            TS.out(new Tuple(
                System.currentTimeMillis() + 100000,
                new Field<>(Integer.class),
                new Field<>(String.class, "Hello my friend")
            ));
        }

        // validate status again
        // (10, "hello my friend")
        System.out.println("AFTER VALIDATING READ TUPLES");
        for (int i = 0; i < 50; ++i) {
            Future<ITuple> tuple = readCalls.get(i);
            try {
               ITuple res = tuple.get();
               IField<String> f0 = res.get(1);
               String message = f0.element();
               System.out.println("The message = " + message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        System.out.println("FINISHED :D");
    }

}
