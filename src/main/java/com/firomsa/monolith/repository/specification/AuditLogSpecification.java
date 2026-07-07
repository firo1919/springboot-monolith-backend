package com.firomsa.monolith.repository.specification;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.firomsa.monolith.dto.AuditLogFilterDTO;
import com.firomsa.monolith.model.AuditLog;

import jakarta.persistence.criteria.Predicate;

public final class AuditLogSpecification {

    private AuditLogSpecification() {
    }

    /**
     * Builds a dynamic {@link Specification} that adds a predicate only for each
     * non-null filter field. This avoids the
     * {@code (:param IS NULL OR col = :param)}
     * idiom, which PostgreSQL cannot type-infer when a parameter is bound as NULL.
     */
    public static Specification<AuditLog> withFilters(AuditLogFilterDTO filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.correlationId() != null) {
                predicates.add(cb.equal(root.get("correlationId"), filter.correlationId()));
            }
            if (filter.userId() != null) {
                predicates.add(cb.equal(root.get("userId"), filter.userId()));
            }
            if (filter.username() != null) {
                predicates.add(cb.equal(root.get("username"), filter.username()));
            }
            if (filter.action() != null) {
                predicates.add(cb.equal(root.get("action"), filter.action()));
            }
            if (filter.resourceType() != null) {
                predicates.add(cb.equal(root.get("resourceType"), filter.resourceType()));
            }
            if (filter.status() != null) {
                predicates.add(cb.equal(root.get("status"), filter.status()));
            }
            if (filter.startDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), filter.startDate()));
            }
            if (filter.endDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("timestamp"), filter.endDate()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
