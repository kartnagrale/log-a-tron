package com.logatron.logquery.domain;
import com.logatron.common.error.ApiException;import org.junit.jupiter.api.Test;import java.time.*;import static org.assertj.core.api.Assertions.*;
class LogSearchCriteriaTest{
 private final Clock clock=Clock.fixed(Instant.parse("2026-08-29T08:00:00Z"),ZoneOffset.UTC);
 @Test void defaultsAreBounded(){var n=empty(null).normalize(clock);assertThat(n.from()).isEqualTo(Instant.parse("2026-08-29T07:30:00Z"));assertThat(n.to()).isEqualTo(clock.instant());assertThat(n.pageSize()).isEqualTo(100);}
 @Test void rejectsMoreThanTwentyFourHours(){assertThatThrownBy(()->new LogSearchCriteria(null,null,null,null,null,clock.instant().minus(Duration.ofHours(25)),clock.instant(),null,null,null,null,null,null,null,null,null,100,null).normalize(clock)).isInstanceOf(ApiException.class);}
 @Test void rejectsDeepPagesAndInvalidTraceIds(){assertThatThrownBy(()->empty(501).normalize(clock)).isInstanceOf(ApiException.class);assertThatThrownBy(()->new LogSearchCriteria(null,null,null,null,null,null,null,"not-a-trace",null,null,null,null,null,null,null,null,100,null).normalize(clock)).isInstanceOf(ApiException.class);}
 private LogSearchCriteria empty(Integer page){return new LogSearchCriteria(null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,page,null);}
}
