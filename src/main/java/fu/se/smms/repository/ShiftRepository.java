package fu.se.smms.repository;

import fu.se.smms.entity.Shift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ShiftRepository extends JpaRepository<Shift, Integer> {
    List<Shift> findByShiftDateBetween(LocalDate startDate, LocalDate endDate);

    List<Shift> findByUser_UserId(Integer userId);

    List<Shift> findByUser_UserIdAndShiftDate(Integer userId, LocalDate shiftDate);

    List<Shift> findByUser_UserIdAndShiftDateGreaterThanEqualOrderByShiftDateAsc(Integer userId, LocalDate fromDate);

    // Search shifts by staff name
    @Query("SELECT s FROM Shift s JOIN s.user u WHERE LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Shift> searchByStaffName(@Param("keyword") String keyword);
}
