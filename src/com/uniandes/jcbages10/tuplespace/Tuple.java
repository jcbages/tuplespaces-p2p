package com.uniandes.jcbages10.tuplespace;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class Tuple implements ITuple {

    /**
     * Fields container
     */
    private List<IField> fields;

    /**
     * Leasing indicating the expiration date of the message
     */
    private long leasing;

    /**
     * Constructor for initializing the fields & the leasing with given args
     * @param leasing The expiration time of the message
     * @param fields The fields part of the tuple
     */
    public Tuple(long leasing, IField... fields) {
        this.fields = Arrays.asList(fields);
        this.leasing = leasing;
    }

    /**
     * Constructor for initializing only the fields & a leasing of 0
     * @param fields The fields part of the tuple
     */
    public Tuple(IField... fields) {
        this.fields = Arrays.asList(fields);
        this.leasing = 0;
    }

    /**
     * Internal constructor for initializing the fields with a given list
     * @param fields The list with the fields part of the tuple
     */
    private Tuple(List<IField> fields) {
        this.fields = fields;
        this.leasing = 0;
    }

    /**
     * Try to match the current tuple with the given one and return the result
     * @param tuple The tuple to try to match with this one
     * @return The matched result or none if tuples don't match
     */
    @Override
    public Optional<ITuple> match(ITuple tuple) {
        // abort if lengths are different
        if (this.length() != tuple.length()) {
            return Optional.empty();
        }

        // abort if any field is not matching
        if (!allFieldsMatch(tuple)) {
            return Optional.empty();
        }

        // build & return the resulting matched tuple
        return Optional.of(getMatchTuple(tuple));
    }

    /**
     * Verify if all fields in this tuple & the given one match,
     * this method assumes both tuples have the same length
     * @param tuple The tuple to compare fields with this one
     * @return True if all fields can be matched, otherwise false
     */
    private boolean allFieldsMatch(ITuple tuple) {
        boolean isMatch = true;
        for (int i = 0; i < this.length() && isMatch; i++) {
            IField field1 = this.get(i);
            IField field2 = tuple.get(i);

            if (field1.isFormal() && field2.isFormal()) {
                isMatch = false;
            } else if (field1.isFormal() || field2.isFormal()) {
                isMatch = field1.type().equals(field2.type());
            } else {
                isMatch = field1.equals(field2);
            }
        }
        return isMatch;
    }

    /**
     * Get the result of matching this tuple with the given one,
     * this method assumes both tuples are matchable
     * @param tuple The tuple to match with this one
     * @return The resulting matched tuple
     */
    private ITuple getMatchTuple(ITuple tuple) {
        List<IField> fields = new ArrayList<>();
        for (int i = 0; i < this.length(); i++) {
            IField field1 = this.get(i);
            IField field2 = tuple.get(i);
            IField resultField = getMatchField(field1, field2);
            fields.add(resultField);
        }

        return new Tuple(fields);
    }

    /**
     * Get the result of matching two given fields,
     * this method assumes both fields are matchable
     * @param field1 The first field to match
     * @param field2 The second field to match
     * @return The resulting matched field
     */
    private IField getMatchField(IField field1, IField field2) {
        return field1.isFormal() ? field2 : field1;
    }

    /**
     * Return the field at the given position of the tuple
     * @param position The position of the field
     * @return The field at the given position
     */
    @Override
    public IField get(int position) {
        return this.fields.get(position);
    }

    /**
     * Return the length of the tuple, that is, the number of fields
     * @return The length of the tuple
     */
    @Override
    public int length() {
        return this.fields.size();
    }

    /**
     * Return the leasing or expiration time of the tuple
     * @return The leasing time of the tuple
     */
    @Override
    public long leasing() {
        return this.leasing;
    }

    /**
     * Pretty print format for tuple
     */
    @Override
    public String toString() {
        StringBuilder string = new StringBuilder();
        string.append("(");
        for (int i = 0; i < this.fields.size(); ++i) {
            IField field = this.fields.get(i);
            string.append(field.toString());
            string.append(", ");
        }
        string.append("leasing = ");
        string.append(this.leasing());
        string.append(")");
        return string.toString();
    }
}
