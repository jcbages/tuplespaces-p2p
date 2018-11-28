package com.uniandes.jcbages10.tuplespace;

public class Field<T> implements IField<T> {

    /**
     * The class type of the field
     */
    private Class<T> type;

    /**
     * The element held by the field
     */
    private T element;

    /**
     * Whether or not the field is a formal
     */
    private boolean isFormal;

    /**
     * Constructor for formal fields, only specify the type
     * @param type The type of the formal field
     */
    public Field(Class<T> type) {
        this.type = type;
        this.isFormal = true;
    }

    /**
     * Constructor for actual fields, specify type + element
     * @param type The type of the actual field
     * @param element The element held by the actual field
     */
    public Field(Class<T> type, T element) {
        this.type = type;
        this.element = element;
        this.isFormal = false;
    }

    /**
     * Return true if the field is a formal, else false
     * @return True if the field is a formal, else false
     */
    @Override
    public boolean isFormal() {
        return isFormal;
    }

    /**
     * Return true if the field is an actual, else false
     * @return True if the field is an actual, else false
     */
    @Override
    public boolean isActual() {
        return !isFormal;
    }

    /**
     * Return the type property of the field
     * @return The field type
     */
    @Override
    public Class<T> type() {
        return this.type;
    }

    /**
     * Return the element held by the field
     * @return The element held by the field
     */
    @Override
    public T element() {
        return this.element;
    }

    /**
     * Check if the given object is equals to this field,
     * this is true iff the given object is a field and if either
     * both fields are formal or both are actual fields of the same type
     * and their elements are equal (calling their equals method)
     * @param object The object to compare
     * @return True if they are equal, else false
     */
    @Override
    public boolean equals(Object object) {
        // abort if no object is given
        if (object == null) {
            return false;
        }

        // abort if object is not a field castable
        if (!IField.class.isAssignableFrom(object.getClass())) {
            return false;
        }

        IField genericField = (IField) object;

        // return true if both are formals
        if (this.isFormal() && genericField.isFormal()) {
            return true;
        }

        // abort if both no actual
        if (!this.isActual() || !genericField.isActual()) {
            return false;
        }

        // abort if both not of same type
        if (this.type() != genericField.type()) {
            return false;
        }

        @SuppressWarnings("unchecked")
        IField<T> field = (IField) object;

        T element1 = this.type().cast(this.element());
        T element2 = field.type().cast(field.element());
        return element1.equals(element2);
    }

    /**
     * Override toString for pretty printing of element
     */
    @Override
    public String toString() {
        return this.element().toString();
    }

}
