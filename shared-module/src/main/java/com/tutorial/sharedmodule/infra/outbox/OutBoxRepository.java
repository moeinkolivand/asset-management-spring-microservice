package com.tutorial.sharedmodule.infra.outbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OutBoxRepository extends JpaRepository<OutBox, UUID> {}
