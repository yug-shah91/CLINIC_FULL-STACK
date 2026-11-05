package com.clinic.config;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

/** Keeps client-supplied sort fields to a known list (page size is capped by spring.data.web.pageable.max-page-size). */
public final class PageRequests {

    private PageRequests() {}

    public static Pageable withAllowedSort(Pageable pageable, Set<String> allowed, Sort fallback) {
        boolean ok = pageable.getSort().stream().allMatch(o -> allowed.contains(o.getProperty()));
        Sort sort = (pageable.getSort().isSorted() && ok) ? pageable.getSort() : fallback;
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }
}
