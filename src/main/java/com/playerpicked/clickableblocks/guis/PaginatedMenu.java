package com.playerpicked.clickableblocks.guis;

import com.playerpicked.clickableblocks.data.MenuController;
import org.jetbrains.annotations.NotNull;

public abstract class PaginatedMenu extends Menu {
    protected int page = 0;

    public PaginatedMenu(@NotNull MenuController menuController) {
        super(menuController);
    }

    protected int maxPage(int total, int perPage) {
        if (perPage <= 0) return 0;
        return Math.max(0, (total - 1) / perPage);
    }

    protected void clampPage(int total, int perPage) {
        page = Math.clamp(page, 0, maxPage(total, perPage));
    }
}