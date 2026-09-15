package com.wovenreviews.preflight;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * One trivial table, so Flyway has a migration to apply and JPA has a mapping to validate.
 * It exercises the same schema path as the real application without meaning anything.
 */
@Entity
@Table(name = "ping")
public class Ping {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "note", nullable = false, length = 50)
    private String note;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
