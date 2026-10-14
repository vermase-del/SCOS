package com.scos.modules.maintenance.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "scos_work_order_sequences")
@IdClass(WorkOrderSequenceEntity.Key.class)
public class WorkOrderSequenceEntity {

    @Id
    @Column(name = "community_id", nullable = false)
    private UUID communityId;

    @Id
    @Column(name = "\"year\"", nullable = false)
    private int year;

    @Column(name = "current_sequence", nullable = false)
    private long currentSequence;

    public UUID getCommunityId() { return communityId; }
    public void setCommunityId(UUID communityId) { this.communityId = communityId; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public long getCurrentSequence() { return currentSequence; }
    public void setCurrentSequence(long currentSequence) { this.currentSequence = currentSequence; }

    public static class Key implements Serializable {
        private UUID communityId;
        private int year;

        public Key() {}
        public Key(UUID communityId, int year) {
            this.communityId = communityId;
            this.year = year;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key k)) return false;
            return year == k.year && Objects.equals(communityId, k.communityId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(communityId, year);
        }
    }
}
