package com.scarlxrd.order_service.entity;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "processed_events")
@NoArgsConstructor
@AllArgsConstructor
@Data
public class ProcessedEvent {

    @Id
    private String eventId;
}