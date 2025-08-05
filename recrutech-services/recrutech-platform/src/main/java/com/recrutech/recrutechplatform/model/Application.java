package com.recrutech.recrutechplatform.model;

import com.recrutech.common.entity.BaseEntity;
import com.recrutech.recrutechplatform.enums.ApplicationStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name="application")
@Getter
@Setter
public class Application extends BaseEntity {

    @Column(name = "cv_file_id", columnDefinition = "char", length = 36)
    private String cvFileId;

    @Column(name = "user_id", columnDefinition = "char", length = 36)
    private String userId;

    @Enumerated(EnumType.STRING)
    private ApplicationStatus status;

    private boolean viewedByHr;

    @ManyToOne
    @JoinColumn(name = "job_id")
    private Job job;

    @PrePersist
    protected void onCreate() {
        initializeEntity();
    }
}
