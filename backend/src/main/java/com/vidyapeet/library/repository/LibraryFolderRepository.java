package com.vidyapeet.library.repository;

import com.vidyapeet.library.LibraryFolder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LibraryFolderRepository extends JpaRepository<LibraryFolder, Long> {

    List<LibraryFolder> findAllByOrderByNameAsc();

    void deleteByInstituteId(Long instituteId);
}
