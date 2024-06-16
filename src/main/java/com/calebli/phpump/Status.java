package com.calebli.phpump;

public enum Status {
    STOP_READY, RUN, DISCONNECTED, INTERRUPTED;

    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }
}
