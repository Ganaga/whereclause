package org.ganaga.whereclause;

public class WhereClauseException extends Exception {

    private static final long serialVersionUID = 1L;

    public WhereClauseException(String msg) {
        super(msg);
    }

    public WhereClauseException(String msg, Throwable t) {
        super(msg, t);
    }
}