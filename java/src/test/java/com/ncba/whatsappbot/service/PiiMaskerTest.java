package com.ncba.whatsappbot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PiiMaskerTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "254712345678|********5678",
            "1234|****",
            "99|**"
    })
    void maskPhone_masks_all_but_last_four(String input, String expected) {
        assertEquals(expected, PiiMasker.maskPhone(input));
    }

    @Test
    void maskPhone_empty_returns_unknown() {
        assertEquals("(unknown)", PiiMasker.maskPhone(""));
    }

    @Test
    void maskPhone_null_returns_unknown() {
        assertEquals("(unknown)", PiiMasker.maskPhone(null));
    }
}
