package com.logatron.logquery.web;
import com.logatron.logquery.application.SavedSearchService;import com.logatron.logquery.web.SavedSearchDtos.*;import jakarta.servlet.http.HttpServletRequest;import org.springframework.http.HttpStatus;import org.springframework.security.core.annotation.AuthenticationPrincipal;import org.springframework.security.oauth2.jwt.Jwt;import org.springframework.web.bind.annotation.*;import java.util.*;
@RestController @RequestMapping("/api/v1/saved-searches")
public class SavedSearchController{
 private final SavedSearchService service;public SavedSearchController(SavedSearchService service){this.service=service;}
 @GetMapping public List<SavedSearchView> list(@AuthenticationPrincipal Jwt jwt){return service.list(jwt);}@GetMapping("/{id}")public SavedSearchView get(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id){return service.get(jwt,id);}
 @PostMapping @ResponseStatus(HttpStatus.CREATED)public SavedSearchView create(@AuthenticationPrincipal Jwt jwt,@RequestBody SaveRequest request,HttpServletRequest http){return service.create(jwt,request,http);}
 @PutMapping("/{id}")public SavedSearchView update(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@RequestBody SaveRequest request,HttpServletRequest http){return service.update(jwt,id,request,http);}
 @DeleteMapping("/{id}")@ResponseStatus(HttpStatus.NO_CONTENT)public void delete(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,HttpServletRequest http){service.delete(jwt,id,http);}
}
