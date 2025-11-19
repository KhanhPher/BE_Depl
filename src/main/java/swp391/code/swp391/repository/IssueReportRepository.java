package swp391.code.swp391.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swp391.code.swp391.entity.IssueReport;

@Repository
public interface IssueReportRepository extends JpaRepository<IssueReport, Long> {


}
