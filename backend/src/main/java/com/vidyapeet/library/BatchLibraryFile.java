package com.vidyapeet.library;

import com.vidyapeet.tenant.TenantBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;

/**
 * Assignment of a library file to a batch (shared, not copied). Students of the
 * batch can view/download the file.
 */
@Entity
@Table(
        name = "batch_library_files",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_batch_library_file",
                columnNames = {"batch_id", "library_file_id"}
        )
)
@Filter(name = TenantBaseEntity.TENANT_FILTER, condition = TenantBaseEntity.TENANT_CONDITION)
@Getter
@Setter
public class BatchLibraryFile extends TenantBaseEntity {

    @Column(name = "batch_id", nullable = false)
    private Long batchId;

    @Column(name = "library_file_id", nullable = false)
    private Long libraryFileId;
}
