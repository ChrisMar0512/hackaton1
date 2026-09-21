package com.tuckersoft.branchengine.controller;

import com.tuckersoft.branchengine.dto.request.StoryNodeRequest;
import com.tuckersoft.branchengine.dto.response.StoryNodeResponse;
import com.tuckersoft.branchengine.service.StoryNodeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/nodes")
@RequiredArgsConstructor
public class StoryNodeController {

    private final StoryNodeService storyNodeService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StoryNodeResponse> createNode(@Valid @RequestBody StoryNodeRequest request) {
        StoryNodeResponse response = storyNodeService.createNode(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<StoryNodeResponse>> getAllNodes() {
        List<StoryNodeResponse> responses = storyNodeService.getAllNodes();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<StoryNodeResponse> getNodeById(@PathVariable Long id) {
        StoryNodeResponse response = storyNodeService.getNodeById(id);
        return ResponseEntity.ok(response);
    }
}
