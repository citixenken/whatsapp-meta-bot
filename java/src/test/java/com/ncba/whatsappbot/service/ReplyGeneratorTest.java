package com.ncba.whatsappbot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ReplyGeneratorTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "hi|Hello 👋 Welcome to Fintech MVP Bot",
            "Hello there|Hello 👋 Welcome to Fintech MVP Bot",
            "what is my BALANCE|Your balance feature is coming soon 🚧",
            "I need a loan|Loan services will be available in next phase 📊",
            "xyz|I received your message 👍 (MVP mode)"
    })
    void generate_returns_expected_reply(String input, String expected) {
        assertEquals(expected, ReplyGenerator.generate(input));
    }
}
