package com.tuckersoft.branchengine.service;

import com.tuckersoft.branchengine.dto.request.StoryNodeRequest;
import com.tuckersoft.branchengine.dto.response.StoryNodeResponse;
import com.tuckersoft.branchengine.exception.DuplicateResourceException;
import com.tuckersoft.branchengine.exception.ResourceNotFoundException;
import com.tuckersoft.branchengine.model.StoryNode;
import com.tuckersoft.branchengine.repository.StoryNodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StoryNodeService {

    private final StoryNodeRepository storyNodeRepository;

    @Transactional
    public StoryNodeResponse createNode(StoryNodeRequest request) {
        if (storyNodeRepository.existsByNodeCode(request.getNodeCode())) {
            throw new DuplicateResourceException("El nodeCode ya se encuentra registrado: " + request.getNodeCode());
        }

        StoryNode node = StoryNode.builder()
                .nodeCode(request.getNodeCode())
                .title(request.getTitle())
                .sceneText(request.getSceneText())
                .branchCapacity(request.getBranchCapacity())
                .currentBranches(0)
                .primaryBranchCode(request.getPrimaryBranchCode())
                .glitchBranchCode(request.getGlitchBranchCode())
                .createdAt(Instant.now())
                .build();

        storyNodeRepository.save(node);

        return toResponse(node);
    }

    public List<StoryNodeResponse> getAllNodes() {
        return storyNodeRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public StoryNodeResponse getNodeById(Long id) {
        StoryNode node = storyNodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Nodo no encontrado con id: " + id));
        return toResponse(node);
    }

    public StoryNodeResponse toResponse(StoryNode node) {
        return StoryNodeResponse.builder()
                .id(node.getId())
                .nodeCode(node.getNodeCode())
                .title(node.getTitle())
                .sceneText(node.getSceneText())
                .branchCapacity(node.getBranchCapacity())
                .currentBranches(node.getCurrentBranches())
                .primaryBranchCode(node.getPrimaryBranchCode())
                .glitchBranchCode(node.getGlitchBranchCode())
                .createdAt(node.getCreatedAt())
                .build();
    }
}
