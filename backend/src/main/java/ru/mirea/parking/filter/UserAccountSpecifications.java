package ru.mirea.parking.filter;

import org.springframework.data.jpa.domain.Specification;
import ru.mirea.parking.domain.UserAccount;

public final class UserAccountSpecifications {

    private UserAccountSpecifications() {
    }

    public static Specification<UserAccount> textSearch(String text) {
        if (text == null || text.isBlank()) {
            return (root, query, cb) -> cb.conjunction();
        }
        String pattern = LikePattern.contains(text);
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("login")), pattern, LikePattern.ESCAPE),
                cb.like(cb.lower(root.get("displayName")), pattern, LikePattern.ESCAPE));
    }
}
