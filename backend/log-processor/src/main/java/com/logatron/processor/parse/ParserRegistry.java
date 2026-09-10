package com.logatron.processor.parse;

import com.logatron.contracts.ingestion.ParserProfile;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class ParserRegistry {
    private final Map<ParserProfile, LogParser> parsers;

    public ParserRegistry(List<LogParser> available) {
        EnumMap<ParserProfile, LogParser> resolved = new EnumMap<>(ParserProfile.class);
        for (ParserProfile profile : ParserProfile.values()) {
            List<LogParser> matches = available.stream().filter(parser -> parser.supports(profile)).toList();
            if (matches.size() > 1) {
                throw new IllegalStateException("Multiple parsers support profile " + profile);
            }
            if (matches.size() == 1) resolved.put(profile, matches.get(0));
        }
        this.parsers = Map.copyOf(resolved);
    }

    public LogParser require(ParserProfile profile) {
        LogParser parser = parsers.get(profile);
        if (parser == null) {
            throw new ProcessingException("PARSER_NOT_FOUND", "No parser supports the registered profile " + profile);
        }
        return parser;
    }
}
