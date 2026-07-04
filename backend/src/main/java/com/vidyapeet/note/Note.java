package com.vidyapeet.note;

import com.vidyapeet.tenant.TenantBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;

@Entity
@Table(name = "notes")
@Filter(name = TenantBaseEntity.TENANT_FILTER, condition = TenantBaseEntity.TENANT_CONDITION)
@Getter
@Setter
public class Note extends TenantBaseEntity {

    @Column(name = "batch_id", nullable = false)
    private Long batchId;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false)
    private String title;

    /** URL of the stored PDF (Supabase Storage in production). */
    @Column(name = "file_url", nullable = false)
    private String fileUrl;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "uploaded_by")
    private Long uploadedBy;
}
