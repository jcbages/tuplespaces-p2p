package com.uniandes.jcbages10.tuplespace;

import com.uniandes.jcbages10.routing.IRouting;
import com.uniandes.jcbages10.routing.Routing;

import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class TupleSpace implements ITupleSpace {

    /**
     * Tuples container fixed size (1M)
     */
    private final static int TUPLE_SPACE_SIZE = 1000000;

    /**
     * Thread pool fixed size, determines max number of unresolved calls to TS
     */
    private final static int MAX_CALLS_SIZE = 50;

    /**
     * Singleton instance of the TS
     */
    private static ITupleSpace instance;

    /**
     * Container of the tuples, initially full of Optional.empty()
     */
    private List<Optional<ITuple> > tuples;

    /**
     * The number of non-empty Optionals in the tuples container
     */
    private int tuplesSize;

    /**
     * The number of unresolved calls to TS (read or in)
     */
    private int unresolvedCalls;

    /**
     * Random hash of the last tuple insertion
     */
    private UUID lastInsertHash;

    /**
     * Dummy object for blocking/notifying futures
     */
    private final Object futureBlock;

    /**
     * Dummy object for blocking tuple editing
     */
    private final Object editBlock;

    /**
     * Thread pool for threads created by the futures
     */
    private final ExecutorService executor;

    /**
     * Reference to the Routing singleton
     */
    private static IRouting<ITuple> routing = Routing.getInstance();

    /**
     * Private constructor, initialize containers & thread pool
     */
    private TupleSpace() {
        // Initialize tuple container
        this.tuples = new ArrayList<>();
        for (int i = 0; i < TUPLE_SPACE_SIZE; i++) {
            this.tuples.add(Optional.empty());
        }
        this.tuplesSize = 0;
        this.lastInsertHash = UUID.randomUUID();

        // Initialize thread pool
        this.executor = Executors.newFixedThreadPool(MAX_CALLS_SIZE);
        this.unresolvedCalls = 0;

        // Initialize dummy block objects
        this.futureBlock = new Object();
        this.editBlock = new Object();
    }

    /**
     * Get the singleton instance of the TS
     * @return instance of TS
     */
    public static synchronized ITupleSpace getInstance() {
        if (instance == null) {
            instance = new TupleSpace();
        }
        return instance;
    }

    /**
     * Add a tuple to the container. In case container is full,
     * erase the tuple with the oldest leasing time
     * @param tuple The tuple to add
     */
    @Override
    public void out(ITuple tuple) {
        addMultipleTuples(true, tuple);
    }

    /**
     * Add multiple tuples to the container. In case container is full,
     * erase the tuples with the oldest leasing time
     * @param tuples The tuples to add
     */
    @Override
    public void outMany(ITuple... tuples) {
        addMultipleTuples(true, tuples);
    }

    /**
     * Add multiple tuples to the container. Intended to be used by
     * routing layer only as it wont call routing.add() method
     * @param tuples The tuples to add
     */
    @Override
    public void outRouting(ITuple... tuples) {
        addMultipleTuples(false, tuples);
    }

    /**
     * Auxiliary function for both out, outMany & outRouting,
     * call the routing add function if specified
     * @param tuples The tuples to add
     * @param addRouting Whether or not to call routing.add()
     */
    private void addMultipleTuples(boolean addRouting, ITuple... tuples) {
        synchronized (this.editBlock) {
            List<Integer> positions = allocatePositions(tuples.length);
            for (int i = 0; i < positions.size(); ++i) {
                ITuple tuple = tuples[i];
                this.tuples.set(positions.get(i), Optional.of(tuple));
                this.tuplesSize++;

                if (addRouting) {
                    routing.add(tuple);
                }
            }
            this.lastInsertHash = UUID.randomUUID();
        }

        synchronized (this.futureBlock) {
            this.futureBlock.notifyAll();
        }
    }

    /**
     * Return a list of positions with available spots for placing tuples,
     * in case there are not enough empty spaces it will remove oldest tuples
     * according with their leasing values
     * @param numberOfPositions The number of required empty positions
     * @return A list with empty positions
     */
    private List<Integer> allocatePositions(int numberOfPositions) {
        List<Integer> positions = new ArrayList<>();

        if (this.tuplesSize < TUPLE_SPACE_SIZE) {
            positions.addAll(findEmptyPositions(numberOfPositions));
        }

        if (positions.size() < numberOfPositions) {
            int remainingPositions = numberOfPositions - positions.size();
            positions.addAll(eraseTuplesWithOldestLeasing(remainingPositions));
        }

        return positions;
    }

    /**
     * Get the given amount of position with an empty space in the tuple container,
     * it can be the case that it returns less than numberOfPositions if there
     * are not enough empty positions available
     * @return The list of positions with an empty space
     */
    private List<Integer> findEmptyPositions(int numberOfPositions) {
        List<Integer> positions = new ArrayList<>();
        for (int i = 0; i < TUPLE_SPACE_SIZE && positions.size() < numberOfPositions; i++) {
            Optional<ITuple> tuple = this.tuples.get(i);
            if (!tuple.isPresent()) {
                positions.add(i);
            }
        }
        return positions;
    }

    /**
     * Erase the tuples with the oldest leasing time,
     * this method assumes that the tuples container is not empty
     * @return The positions of the erased tuples
     */
    private List<Integer> eraseTuplesWithOldestLeasing(int numberOfPositions) {
        Comparator<ITuple> comparator = new TupleLeasingComparator();
        TreeMap<ITuple, Integer> sortedTuples = new TreeMap<>(comparator);

        for (int i = 0; i < TUPLE_SPACE_SIZE; i++) {
            Optional<ITuple> optionalTuple = this.tuples.get(i);
            if (optionalTuple.isPresent()) {
                ITuple tuple = optionalTuple.get();
                sortedTuples.put(tuple, i);
            }

            if (sortedTuples.size() > numberOfPositions) {
                ITuple tuple = sortedTuples.lastKey();
                sortedTuples.remove(tuple);
            }
        }

        List<Integer> positions = new ArrayList<>(sortedTuples.values());
        for (Integer position : positions) {
            Optional<ITuple> tuple = this.tuples.get(position);
            tuple.ifPresent(routing::remove);
            this.tuples.set(position, Optional.empty());
        }
        return positions;
    }

    /**
     * Remove & return a tuple from the container matching given one
     * @param tuple The tuple to try to match
     * @return The matched tuple
     */
    @Override
    public Future<ITuple> in(ITuple tuple) throws CancellationException {
        return retrieve(tuple, true);
    }

    /**
     * Return a tuple from the container matching given one
     * @param tuple The tuple to try to match
     * @return The matched tuple
     */
    @Override
    public Future<ITuple> read(ITuple tuple) throws CancellationException {
        return retrieve(tuple, false);
    }

    /**
     * Auxiliary function for performing both in & read methods
     * @param tuple The tuple to try to match
     * @param remove Whether or not to remove the matched tuple
     * @return The matched tuple
     */
    private Future<ITuple> retrieve(ITuple tuple, boolean remove) throws CancellationException {
        // abort if unresolved calls is max
        if (!canHandleCall()) {
            throw new CancellationException("Number of unresolved calls is max");
        }

        return executor.submit(() -> {
            Optional<ITuple> result = Optional.empty();
            while (!result.isPresent()) {
                UUID currentInsertHash = this.lastInsertHash;
                result = getMatchingTuple(tuple, remove);
                if (!result.isPresent() && currentInsertHash.equals(this.lastInsertHash)) {
                    synchronized (this.futureBlock) {
                        try {
                            this.futureBlock.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            resolveCall();
            return result.get();
        });
    }

    /**
     * Determines if the max number of unresolved calls was reached,
     * otherwise increase the number of unresolved calls by one
     */
    private synchronized boolean canHandleCall() {
        if (this.unresolvedCalls >= MAX_CALLS_SIZE) {
            return false;
        } else {
            this.unresolvedCalls++;
            return true;
        }
    }

    /**
     * Decrease the number of unresolved calls by one
     */
    private synchronized void resolveCall() {
        this.unresolvedCalls = Math.max(0, this.unresolvedCalls - 1);
    }

    /**
     * Find a tuple matching the given one and return the matched result,
     * in case remove is set to true remove it from the container
     * @param tuple The tuple to try to match
     * @param remove Whether or not to remove the matched tuple
     * @return The matched tuple
     */
    private Optional<ITuple> getMatchingTuple(ITuple tuple, boolean remove) {
        Optional<ITuple> result = Optional.empty();
        for (int i = 0; i < TUPLE_SPACE_SIZE && !result.isPresent(); i++) {
            Optional<ITuple> existingTuple = this.tuples.get(i);
            if (existingTuple.isPresent() && isTupleRelevant(existingTuple.get())) {
                result = existingTuple.get().match(tuple);
                if (result.isPresent() && remove && !tryRemovingTuple(i)) {
                    result = Optional.empty();
                }
            }
        }
        return result;
    }

    /**
     * Determines if the given tuple is still relevant regarding its leasing time
     * @param tuple The tuple to validate
     * @return true if the tuple is still relevant, otherwise true
     */
    private boolean isTupleRelevant(ITuple tuple) {
        long currentTime = System.currentTimeMillis();
        return tuple.leasing() >= currentTime;
    }

    /**
     * Try to remove the tuple at the given position from the container,
     * in case it was already removed by other process return false
     * @param position The position of the tuple to remove
     * @return True or false depending on if the tuple was removed or already empty
     */
    private boolean tryRemovingTuple(int position) {
        synchronized (this.editBlock) {
            Optional<ITuple> tuple = this.tuples.get(position);
            if (tuple.isPresent()) {
                routing.remove(tuple.get());
                this.tuples.set(position, Optional.empty());
                return true;
            } else {
                return false;
            }
        }
    }

}
