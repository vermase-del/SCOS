package com.scos.modules.maintenance.entity;
import jakarta.persistence.*; import java.io.Serializable; import java.util.UUID;
@Entity @Table(name="scos_work_order_sequences") @IdClass(WorkOrderSequenceEntity.Key.class)
public class WorkOrderSequenceEntity {
 @Id @Column(name="community_id",nullable=false) private UUID communityId; @Id @Column(nullable=false) private int year; @Column(name="current_sequence",nullable=false) private long currentSequence;
 public UUID getCommunityId(){return communityId;} public void setCommunityId(UUID v){communityId=v;} public int getYear(){return year;} public void setYear(int v){year=v;} public long getCurrentSequence(){return currentSequence;} public void setCurrentSequence(long v){currentSequence=v;}
 public static class Key implements Serializable { private UUID communityId; private int year; public Key(){} public Key(UUID c,int y){communityId=c;year=y;} @Override public boolean equals(Object o){if(this==o)return true;if(!(o instanceof Key k))return false;return year==k.year&&java.util.Objects.equals(communityId,k.communityId);}@Override public int hashCode(){return java.util.Objects.hash(communityId,year);} }
}
