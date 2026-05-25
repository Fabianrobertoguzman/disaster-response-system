package com.coit20258.drs.dao;

import java.util.List;
import java.util.Optional;

import com.coit20258.drs.model.DisasterReport;

public interface DisasterReportDao {

    DisasterReport create(DisasterReport report);

    List<DisasterReport> findAll();

    Optional<DisasterReport> findById(int id);

    List<DisasterReport> findByReporterId(int reporterId);

    boolean updateStatus(int id, String status);
}
