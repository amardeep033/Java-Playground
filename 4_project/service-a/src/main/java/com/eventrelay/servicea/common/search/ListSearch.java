package com.eventrelay.servicea.common.search;

import jakarta.persistence.criteria.Predicate;
import java.util.*;
import org.hibernate.query.criteria.JpaExpression;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class ListSearch {
    private ListSearch() {
    }

    public static List<String> choices(String value, Set<String> allowed) {
        if (value == null || value.isBlank())
            return List.of();
        var selected = Arrays.stream(value.split(",", -1)).map(String::trim).distinct().toList();
        if (value.length() > 500 || selected.size() > allowed.size() || !allowed.containsAll(selected))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid column filter");
        return selected;
    }

    public static String text(String q) {
        if (q == null)
            return "";
        if (q.length() > 200)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Search must be at most 200 characters");
        return q.strip().toLowerCase(Locale.ROOT);
    }

    public static String pattern(String q) {
        return "%" + text(q).replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
    }

    public static <T> Specification<T> scoped(String ownerField, UUID owner, String q, String... fields) {
        String search = text(q);
        return (root, query, cb) -> {
            var scope = cb.equal(root.get(ownerField), owner);
            if (search.isEmpty())
                return scope;
            var matches = new ArrayList<Predicate>();
            var id = ((JpaExpression<?>) root.get("id")).cast(String.class);
            matches.add(cb.like(cb.lower(id), pattern(search), '!'));
            for (String field : fields)
                matches.add(cb.like(cb.lower(root.get(field)), pattern(search), '!'));
            return cb.and(scope, cb.or(matches.toArray(Predicate[]::new)));
        };
    }

    public static <T> Specification<T> activationFilter(
            Specification<T> spec, String status, String field) {
        var selected = ListSearch.choices(status, Set.of("ACTIVE", "INACTIVE", "ENABLED", "DISABLED"));
        if (selected.isEmpty())
            return spec;
        var flags = selected.stream().map(value -> Set.of("ACTIVE", "ENABLED").contains(value)).distinct().toList();
        return spec.and((root, query, cb) -> root.get(field).in(flags));
    }
}
