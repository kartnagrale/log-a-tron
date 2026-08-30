package com.logatron.processor.consume;
import com.logatron.contracts.ingestion.*;
public record ProcessingOutcome(CanonicalLogEvent event,DeadLetterEvent deadLetter,boolean duplicate){public static ProcessingOutcome event(CanonicalLogEvent e,boolean d){return new ProcessingOutcome(e,null,d);}public static ProcessingOutcome dead(DeadLetterEvent d){return new ProcessingOutcome(null,d,false);}}