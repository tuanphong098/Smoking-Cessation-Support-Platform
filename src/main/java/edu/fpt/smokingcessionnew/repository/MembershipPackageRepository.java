package edu.fpt.smokingcessionnew.repository;

import edu.fpt.smokingcessionnew.entity.MembershipPackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MembershipPackageRepository extends JpaRepository<MembershipPackage, Integer> {
    List<MembershipPackage> findByIsActiveTrue();
    List<MembershipPackage> findByIsActiveTrueOrderByPriceAsc();
}
