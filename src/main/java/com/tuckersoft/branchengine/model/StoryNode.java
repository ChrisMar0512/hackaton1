package com.tuckersoft.branchengine.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "story_nodes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoryNode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String nodeCode;

    @Column(nullable = false, length = 80)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String sceneText;

    @Column(nullable = false)
    private Integer branchCapacity;

    @Column(nullable = false)
    @Builder.Default
    private Integer currentBranches = 0;

    @Column(length = 40)
    private String primaryBranchCode;

    @Column(length = 40)
    private String glitchBranchCode;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "currentNode", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Playthrough> playthroughs = new ArrayList<>();

    @OneToMany(mappedBy = "node", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Decision> decisions = new ArrayList<>();
}
