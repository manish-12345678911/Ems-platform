package com.h8.ems.common.statemachine;

import com.h8.ems.common.model.UnitStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class UnitStateMachineTest {

    @ParameterizedTest
    @CsvSource({
            "AVAILABLE, DISPATCHED",
            "AVAILABLE, OFFLINE",
            "DISPATCHED, ON_SCENE",
            "DISPATCHED, AVAILABLE",
            "DISPATCHED, OFFLINE",
            "ON_SCENE, TRANSPORTING",
            "ON_SCENE, AVAILABLE",
            "TRANSPORTING, AT_HOSPITAL",
            "AT_HOSPITAL, AVAILABLE",
            "OFFLINE, AVAILABLE"
    })
    void validTransitionsDoNotThrow(String from, String to) {
        assertDoesNotThrow(() ->
                UnitStateMachine.check(UnitStatus.valueOf(from), UnitStatus.valueOf(to)));
    }

    @ParameterizedTest
    @CsvSource({
            "AVAILABLE, ON_SCENE",
            "AVAILABLE, TRANSPORTING",
            "AVAILABLE, AT_HOSPITAL",
            "DISPATCHED, TRANSPORTING",
            "DISPATCHED, AT_HOSPITAL",
            "ON_SCENE, DISPATCHED",
            "ON_SCENE, AT_HOSPITAL",
            "ON_SCENE, OFFLINE",
            "TRANSPORTING, AVAILABLE",
            "TRANSPORTING, DISPATCHED",
            "TRANSPORTING, OFFLINE",
            "AT_HOSPITAL, DISPATCHED",
            "AT_HOSPITAL, OFFLINE",
            "OFFLINE, DISPATCHED",
            "OFFLINE, ON_SCENE"
    })
    void invalidTransitionsThrow(String from, String to) {
        assertThrows(IllegalStateTransitionException.class, () ->
                UnitStateMachine.check(UnitStatus.valueOf(from), UnitStatus.valueOf(to)));
    }

    @Test
    void isValidReturnsTrueForValidTransition() {
        assertTrue(UnitStateMachine.isValid(UnitStatus.AVAILABLE, UnitStatus.DISPATCHED));
    }

    @Test
    void isValidReturnsFalseForInvalidTransition() {
        assertFalse(UnitStateMachine.isValid(UnitStatus.AVAILABLE, UnitStatus.ON_SCENE));
    }
}
