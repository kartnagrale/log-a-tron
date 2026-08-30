package com.logatron.auth.persistence;
import com.logatron.catalog.domain.CatalogEnums.Status;import org.springframework.data.jpa.repository.JpaRepository;import java.util.*;
public interface UserAccountRepository extends JpaRepository<UserAccountEntity,UUID>{Optional<UserAccountEntity> findByIssuerAndSubjectAndStatus(String issuer,String subject,Status status);Optional<UserAccountEntity> findByIssuerAndUsername(String issuer,String username);List<UserAccountEntity> findAllByOrderByDisplayName();}
