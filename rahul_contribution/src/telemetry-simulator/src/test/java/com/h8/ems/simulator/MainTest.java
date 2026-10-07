package com.h8.ems.simulator;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MainTest {

    @Test
    void parseArgsExtractsKeyValuePairs() throws Exception {
        Method parseArgs = Main.class.getDeclaredMethod("parseArgs", String[].class);
        parseArgs.setAccessible(true);

        String[] args = {"--mode=batch", "--scenarios=S1,S2", "--policies=B1,B2,P1", "--reps=5", "--seed=42"};
        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) parseArgs.invoke(null, (Object) args);

        assertEquals("batch", result.get("mode"));
        assertEquals("S1,S2", result.get("scenarios"));
        assertEquals("B1,B2,P1", result.get("policies"));
        assertEquals("5", result.get("reps"));
        assertEquals("42", result.get("seed"));
    }

    @Test
    void parseArgsBooleanFlag() throws Exception {
        Method parseArgs = Main.class.getDeclaredMethod("parseArgs", String[].class);
        parseArgs.setAccessible(true);

        String[] args = {"--verbose"};
        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) parseArgs.invoke(null, (Object) args);

        assertEquals("true", result.get("verbose"));
    }

    @Test
    void parseArgsEmpty() throws Exception {
        Method parseArgs = Main.class.getDeclaredMethod("parseArgs", String[].class);
        parseArgs.setAccessible(true);

        String[] args = {};
        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) parseArgs.invoke(null, (Object) args);

        assertTrue(result.isEmpty());
    }
}
