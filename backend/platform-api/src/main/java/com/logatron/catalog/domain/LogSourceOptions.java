package com.logatron.catalog.domain;

import jakarta.validation.constraints.*;
import java.util.List;

/** Additive receiver settings. Null/legacy values retain the previous defaults. */
public record LogSourceOptions(
        @Size(max=160) String name,
        @Pattern(regexp="end|beginning") String startAt,
        @Pattern(regexp="[1-9][0-9]{0,3}(KiB|MiB)") String maxLogSize,
        @Size(max=20) List<@NotBlank @Size(max=1000) String> includes,
        @Size(max=20) List<@NotBlank @Size(max=1000) String> excludes) {
    public LogSourceOptions {
        name=name==null?"":name;
        startAt=startAt==null?"end":startAt;
        maxLogSize=maxLogSize==null?"200KiB":maxLogSize;
        includes=includes==null?List.of():List.copyOf(includes);
        excludes=excludes==null?List.of():List.copyOf(excludes);
    }
    public static LogSourceOptions defaults(){return new LogSourceOptions(null,null,null,null,null);}
}
