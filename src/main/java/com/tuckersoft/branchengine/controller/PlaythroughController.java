package com.tuckersoft.branchengine.controller;

import com.tuckersoft.branchengine.dto.request.PlaythroughRequest;
import com.tuckersoft.branchengine.dto.response.PlaythroughPathResponse;
import com.tuckersoft.branchengine.dto.response.PlaythroughResponse;
import com.tuckersoft.branchengine.model.User;
import com.tuckersoft.branchengine.service.PlaythroughService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/playthroughs")
@RequiredArgsConstructor
public class PlaythroughController {

    private final PlaythroughService playthroughService;

    @PostMapping
    public ResponseEntity<PlaythroughResponse> createPlaythrough(
            @Valid @RequestBody PlaythroughRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        PlaythroughResponse response = playthroughService.createPlaythrough(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<PlaythroughResponse>> getPlaythroughs(
            @AuthenticationPrincipal User currentUser
    ) {
        List<PlaythroughResponse> responses = playthroughService.getPlaythroughs(currentUser);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlaythroughResponse> getPlaythroughById(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser
    ) {
        PlaythroughResponse response = playthroughService.getPlaythroughById(id, currentUser);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/path")
    public ResponseEntity<PlaythroughPathResponse> getPlaythroughPath(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser
    ) {
        PlaythroughPathResponse response = playthroughService.getPlaythroughPath(id, currentUser);
        return ResponseEntity.ok(response);
    }
}
