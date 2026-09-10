package com.logatron.processor.parse;

import com.logatron.contracts.ingestion.ParserProfile;
import com.logatron.contracts.ingestion.RawLogEnvelope;
import java.time.ZoneId;
import java.time.ZoneOffset;

public interface LogParser {
    boolean supports(ParserProfile profile);

    ParsedLog parse(RawLogEnvelope envelope, ZoneId sourceZone);

    default ParsedLog parse(RawLogEnvelope envelope) {
        return parse(envelope, ZoneOffset.UTC);
    }
}
