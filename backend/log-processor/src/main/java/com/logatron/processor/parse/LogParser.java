package com.logatron.processor.parse;
import com.logatron.contracts.ingestion.*;
public interface LogParser { boolean supports(ParserProfile profile); ParsedLog parse(RawLogEnvelope envelope); }