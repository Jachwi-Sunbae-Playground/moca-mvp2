package com.jachwisunbae.common.page;

import java.util.List;

public record PageResult<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        long totalPages,
        boolean hasNext
) {

    public PageResult {
        content = List.copyOf(content);
    }

    public static <T> PageResult<T> of(
            final List<T> content,
            final PageQuery pageQuery,
            final long totalElements
    ) {
        final long totalPages = totalElements == 0
                ? 0
                : ((totalElements - 1) / pageQuery.size()) + 1;
        final boolean hasNext = (long) pageQuery.page() + 1 < totalPages;
        return new PageResult<>(
                content,
                pageQuery.page(),
                pageQuery.size(),
                totalElements,
                totalPages,
                hasNext
        );
    }
}
