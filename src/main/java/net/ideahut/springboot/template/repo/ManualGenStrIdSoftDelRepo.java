package net.ideahut.springboot.template.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import net.ideahut.springboot.template.entity.ManualGenStrIdSoftDel;

public interface ManualGenStrIdSoftDelRepo extends SoftDeleteRepository<ManualGenStrIdSoftDel, String> {

	@Override
	@Query("select e from #{#entityName} e where e.deletedOn is null and id = :id")
	Optional<ManualGenStrIdSoftDel> findById(@Param("id") String id);
	
}
