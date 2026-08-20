package com.jachwisunbae.checklist.repository.query;

import com.jachwisunbae.checklist.entity.UserChecklistItem;
import lombok.Getter;

@Getter
public class UserChecklistItemDetail {
    private final UserChecklistItem item;

    public UserChecklistItemDetail(final UserChecklistItem item) {
        this.item = item;
    }
}
