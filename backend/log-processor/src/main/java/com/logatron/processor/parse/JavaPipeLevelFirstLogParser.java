package com.logatron.processor.parse;

import com.logatron.contracts.ingestion.ParserProfile;
import com.logatron.contracts.ingestion.RawLogEnvelope;
import org.springframework.stereotype.Component;
import java.time.ZoneId;

/** Reusable parser for: LEVEL | thread | timestamp | source-location | message. */
@Component
public class JavaPipeLevelFirstLogParser implements LogParser {
    private final TimestampNormalizer time;
    public JavaPipeLevelFirstLogParser(TimestampNormalizer time){this.time=time;}
    @Override public boolean supports(ParserProfile profile){return profile==ParserProfile.JAVA_PIPE_LEVEL_FIRST_V1;}
    @Override public ParsedLog parse(RawLogEnvelope envelope,ZoneId sourceZone){
        return JavaPipeParserSupport.parse(envelope,sourceZone,time,1,0,"JAVA_PIPE_LEVEL_FIRST_V1");
    }
}
