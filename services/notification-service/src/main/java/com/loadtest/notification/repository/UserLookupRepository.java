package com.loadtest.notification.repository;

import com.loadtest.notification.domain.UserRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserLookupRepository extends JpaRepository<UserRecord, UUID> {
}
