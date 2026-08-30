package com.logatron.logquery.web;
import com.logatron.logquery.domain.LogSearchCriteria;import java.time.Instant;import java.util.UUID;
public final class SavedSearchDtos{private SavedSearchDtos(){}public record SaveRequest(String name,String visibility,LogSearchCriteria criteria){}public record SavedSearchView(UUID id,UUID ownerUserId,UUID projectId,String name,String visibility,LogSearchCriteria criteria,Instant createdAt,Instant updatedAt){}}
