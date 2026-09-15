package com.wovenreviews.preflight;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface PingRepository extends JpaRepository<Ping, UUID> {}
