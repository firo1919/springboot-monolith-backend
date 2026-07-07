package com.firomsa.monolith.v1.dto;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageRequest {
    @Builder.Default
    private int page = 0;

    @Builder.Default
    private int size = 10;

    private String sortBy;

    @Builder.Default
    private Sort.Direction sortDirection = Sort.Direction.ASC;

    public Pageable toPageable() {
        Sort sort = sortBy != null ? Sort.by(sortDirection, sortBy) : Sort.unsorted();
        return org.springframework.data.domain.PageRequest.of(page, size, sort);
    }
}
