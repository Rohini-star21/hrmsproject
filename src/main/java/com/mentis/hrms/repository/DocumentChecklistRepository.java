package com.mentis.hrms.repository;

import com.mentis.hrms.model.DocumentChecklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentChecklistRepository extends JpaRepository<DocumentChecklist, Long> {
    List<DocumentChecklist> findAllByOrderByDisplayOrderAsc();
    Optional<DocumentChecklist> findByDocumentType(String documentType);
    List<DocumentChecklist> findByCategoryOrderByDisplayOrderAsc(String category);
    List<DocumentChecklist> findByMandatoryTrueOrderByDisplayOrderAsc();
}