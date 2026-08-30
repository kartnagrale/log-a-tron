package com.logatron.catalog.web;
import com.logatron.catalog.domain.CatalogEnums.*;import jakarta.validation.Validation;import org.junit.jupiter.api.Test;import java.util.*;import static org.assertj.core.api.Assertions.*;
class CatalogValidationTest{@Test void rejectsInvalidServiceKey(){var validator=Validation.buildDefaultValidatorFactory().getValidator();var request=new CatalogDtos.CreateServiceRequest(UUID.randomUUID(),"INVALID KEY","Name",null,Criticality.HIGH,Map.of());assertThat(validator.validate(request)).extracting(v->v.getPropertyPath().toString()).contains("serviceKey");}}
