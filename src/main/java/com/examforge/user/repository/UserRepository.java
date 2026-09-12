package com.examforge.user.repository;

import com.examforge.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByMobile(String mobile);

    long countByCreatedAtGreaterThanEqual(Instant since);

    @Query("select cast(u.createdAt as date) as day, count(u) as count " +
            "from User u where u.createdAt >= :since group by cast(u.createdAt as date) order by day asc")
    List<RegistrationDayProjection> findRegistrationTrend(@Param("since") Instant since);

    interface RegistrationDayProjection {
        LocalDate getDay();
        Long getCount();
    }
}
