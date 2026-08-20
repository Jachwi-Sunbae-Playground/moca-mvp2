package com.jachwisunbae.checklist.repository.query;

import com.jachwisunbae.checklist.entity.SystemCheckItem;
import com.jachwisunbae.checklist.entity.UserChecklistItem;
import lombok.Getter;

@Getter
public class UserChecklistItemDetail {
    private final UserChecklistItem item;
    private final SystemCheckItem systemCheckItem;

    public UserChecklistItemDetail(final UserChecklistItem item, final SystemCheckItem systemCheckItem) {
        this.item = item;
        this.systemCheckItem = systemCheckItem;
    }
}
