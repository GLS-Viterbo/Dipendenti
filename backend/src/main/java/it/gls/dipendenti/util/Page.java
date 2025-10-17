package it.gls.dipendenti.util;

import java.util.List;

public record Page<T>(
        List<T> content,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages
) {
    public Page(List<T> content, int pageNumber, int pageSize, long totalElements) {
        this(
                content,
                pageNumber,
                pageSize,
                totalElements,
                (int) Math.ceil((double) totalElements / pageSize)
        );
    }

    public boolean hasNext() {
        return pageNumber < totalPages - 1;
    }

    public boolean hasPrevious() {
        return pageNumber > 0;
    }

    public boolean isEmpty() {
        return content.isEmpty();
    }
}
