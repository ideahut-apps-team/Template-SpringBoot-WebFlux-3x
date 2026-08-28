package net.ideahut.springboot.template.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import net.ideahut.springboot.template.entity.LongIdJoinComposite;

public interface LongIdJoinCompositeRepo extends SoftDeleteRepository<LongIdJoinComposite, Long> {

	@Override
	@Query("select e from #{#entityName} e where e.deletedOn is null and id = :id")
	Optional<LongIdJoinComposite> findById(@Param("id") Long id);
	
}
