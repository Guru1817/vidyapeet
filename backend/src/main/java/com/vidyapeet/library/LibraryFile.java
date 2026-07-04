package com.vidyapeet.library;

import com.vidyapeet.tenant.TenantBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;

/**
 * A PDF stored in a library folder. Shared with batches via
 * {@link BatchLibraryFile}; the underlying file is served through an
 * access-controlled download endpoint.
 */
@Entity
@Table(name = "library_files")
@Filter(name = TenantBaseEntity.TENANT_FILTER, condition = TenantBaseEntity.TENANT_CONDITION)
@Getter
@Setter
public class LibraryFile extends TenantBaseEntity {

    @Column(name = "folder_id", nullable = false)
    private Long folderId;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false)
    private String title;

    @Column(name = "file_url", nullable = false)
    private String fileUrl;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "uploaded_by")
    private Long uploadedBy;
}
