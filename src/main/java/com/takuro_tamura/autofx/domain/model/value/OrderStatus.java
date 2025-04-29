package com.takuro_tamura.autofx.domain.model.value;

import com.google.common.collect.Sets;

import java.util.Set;

public enum OrderStatus {
    WAITING, FILLED, CLOSED, CANCELED,
    ;

    public boolean isCompleted() {
        return Group.COMPLETED.contains(this);
    }

    public static class Group {
        public static Set<OrderStatus> COMPLETED = Sets.immutableEnumSet(CLOSED, CANCELED);
    }
}
