package com.cssms.controller;

import com.cssms.dto.InventoryDto;
import com.cssms.dto.DashboardDto;
import com.cssms.security.Access;
import com.cssms.service.DashboardService;
import com.cssms.service.InventoryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Inventory & Dashboard")
@RestController
@RequiredArgsConstructor
@PreAuthorize(Access.ALL_ROLES)
public class InventoryController {

    private final InventoryService inventoryService;
    private final DashboardService dashboardService;

    @GetMapping("/api/inventory")
    public List<InventoryDto.Response> inventory(@RequestParam(required = false) String search) {
        return inventoryService.list(search);
    }

    @GetMapping("/api/dashboard")
    public DashboardDto.Response dashboard() {
        return dashboardService.get();
    }
}
