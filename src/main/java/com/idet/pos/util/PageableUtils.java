package com.idet.pos.util;

import com.idet.pos.exception.InvalidRequestException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;
import java.util.TreeSet;

/**
 * Helpers for paginated endpoints.
 * Sort properties are checked against a whitelist so an unknown property is a 400, never a 500,
 * and native/report queries get a Pageable without a sort because they keep their own ORDER BY.
 */
public final class PageableUtils {

    public static final int DEFAULT_PAGE_SIZE = 20;

    private PageableUtils() {
    }

    public static Pageable requireSortable(Pageable pageable, Set<String> allowedProperties) {
        for (Sort.Order order : pageable.getSort()) {
            if (!allowedProperties.contains(order.getProperty())) {
                throw new InvalidRequestException("Unsupported sort property: " + order.getProperty()
                        + ". Allowed: " + String.join(", ", new TreeSet<>(allowedProperties)));
            }
        }
        return pageable;
    }

    public static Pageable withoutSort(Pageable pageable) {
        if (pageable.isUnpaged()) {
            return pageable;
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
    }
}
