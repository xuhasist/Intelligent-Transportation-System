package com.demo.repository.dynamic;

import com.demo.model.dynamic.DynamicCsvFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DynamicCsvFileRepository extends JpaRepository<DynamicCsvFile, Integer> {
}
