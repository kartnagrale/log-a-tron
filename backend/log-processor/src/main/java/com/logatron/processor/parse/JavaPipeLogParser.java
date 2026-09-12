package com.logatron.processor.parse;

import com.logatron.contracts.ingestion.ParserProfile;
import com.logatron.contracts.ingestion.RawLogEnvelope;
import org.springframework.stereotype.Component;
import java.time.ZoneId;

/** Reusable parser for: thread | LEVEL | timestamp | source-location | message. */
@Component
public class JavaPipeLogParser implements LogParser {
    private final TimestampNormalizer time;
    public JavaPipeLogParser(TimestampNormalizer time){this.time=time;}
    @Override public boolean supports(ParserProfile profile){return profile==ParserProfile.JAVA_PIPE_V1;}
    @Override public ParsedLog parse(RawLogEnvelope envelope,ZoneId sourceZone){
        return JavaPipeParserSupport.parse(envelope,sourceZone,time,0,1,"JAVA_PIPE_V1");
    }
}
