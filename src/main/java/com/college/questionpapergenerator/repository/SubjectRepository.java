package com.college.questionpapergenerator.repository;

import com.college.questionpapergenerator.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubjectRepository extends JpaRepository<Subject, Long> {

    Optional<Subject> findBySubjectCode(String subjectCode);

    List<Subject> findBySubjectNameIgnoreCase(String subjectName);
}
