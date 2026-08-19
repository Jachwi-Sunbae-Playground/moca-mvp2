package com.jachwisunbae.checklist.controller;

import com.jachwisunbae.checklist.controller.dto.request.SystemCheckItemSearchRequest;
import com.jachwisunbae.checklist.controller.dto.response.SystemCheckItemResponse;
import com.jachwisunbae.checklist.service.SystemCheckItemService;
import com.jachwisunbae.common.web.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/check-items")
public class SystemCheckItemController {

    private final SystemCheckItemService systemCheckItemService;

    public SystemCheckItemController(final SystemCheckItemService systemCheckItemService) {
        this.systemCheckItemService = systemCheckItemService;
    }

    @GetMapping
    public ApiResponse<List<SystemCheckItemResponse>> search(
            @Valid @ModelAttribute final SystemCheckItemSearchRequest request) {
        return ApiResponse.of(systemCheckItemService.search(request.stage(), request.query()).stream()
                .map(SystemCheckItemResponse::from)
                .toList());
    }
}
