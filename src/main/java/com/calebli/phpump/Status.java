package com.calebli.phpump;

public enum Status {
    STOP_READY, RUN, NOT_CONNECTED;

    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }
}
