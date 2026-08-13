package com.jachwisunbae.common.page;

import java.util.List;

public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        long totalPages,
        boolean hasNext
) {

    public static <S, T> PageResponse<T> from(
            final PageResult<S> result,
            final java.util.function.Function<S, T> mapper
    ) {
        return new PageResponse<>(
                result.content().stream().map(mapper).toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages(),
                result.hasNext()
        );
    }
}
