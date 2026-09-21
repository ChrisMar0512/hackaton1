package com.tuckersoft.branchengine.service;

import com.tuckersoft.branchengine.dto.request.DecisionRequest;
import com.tuckersoft.branchengine.dto.response.DecisionResponse;
import com.tuckersoft.branchengine.event.DecisionCommittedEvent;
import com.tuckersoft.branchengine.model.Decision;
import com.tuckersoft.branchengine.model.Playthrough;
import com.tuckersoft.branchengine.model.StoryNode;
import com.tuckersoft.branchengine.model.User;
import com.tuckersoft.branchengine.repository.DecisionRepository;
import com.tuckersoft.branchengine.repository.PlaythroughRepository;
import com.tuckersoft.branchengine.repository.StoryNodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DecisionServiceTest {

    @Mock
    private DecisionRepository decisionRepository;

    @Mock
    private PlaythroughRepository playthroughRepository;

    @Mock
    private StoryNodeRepository storyNodeRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private DecisionService decisionService;

    private User testUser;
    private StoryNode sourceNode;
    private StoryNode targetNode;
    private Playthrough playthrough;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("stefan@tuckersoft.co.uk")
                .displayName("Stefan Butler")
                .role("ROLE_USER")
                .build();

        sourceNode = StoryNode.builder()
                .id(10L)
                .nodeCode("NODE-DESAYUNO")
                .title("Escena Desayuno")
                .sceneText("Stefan esta decidiendo que cereal comer.")
                .branchCapacity(5)
                .currentBranches(1)
                .primaryBranchCode("NODE-BUS")
                .glitchBranchCode("NODE-ESPEJO")
                .createdAt(Instant.now())
                .build();

        targetNode = StoryNode.builder()
                .id(20L)
                .nodeCode("NODE-ESPEJO")
                .title("Escena Espejo")
                .sceneText("Stefan mira fijamente el reflejo en el espejo.")
                .branchCapacity(5)
                .currentBranches(0)
                .createdAt(Instant.now())
                .build();

        playthrough = Playthrough.builder()
                .id(100L)
                .playerTag("STEFAN-QA-01")
                .user(testUser)
                .startNodeCode("NODE-DESAYUNO")
                .currentNode(sourceNode)
                .lucidity(100)
                .controlLevel(0)
                .status("ACTIVA")
                .endingCode(null)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    @DisplayName("1. Precedencia de reglas: 'Stefan destruye la camara' clasifica como RUPTURA_CUARTA_PARED")
    void testPrecedenciaReglaRupturaCuartaPared() {
        String texto = "Stefan destruye la camara";
        String rama = decisionService.classifyBranch(texto);

        assertEquals("RUPTURA_CUARTA_PARED", rama,
                "Regla 2 (camara) debe evaluarse antes que Regla 4 (destruye)");
    }

    @Test
    @DisplayName("2. Entrada sin letras clasifica como ENTRADA_CORRUPTA y la partida no se modifica")
    void testEntradaCorruptaNoModificaPartida() {
        when(playthroughRepository.findById(100L)).thenReturn(Optional.of(playthrough));
        when(decisionRepository.save(any(Decision.class))).thenAnswer(invocation -> {
            Decision d = invocation.getArgument(0);
            d.setId(501L);
            return d;
        });

        DecisionRequest request = new DecisionRequest(100L, "%%% 0100101 @@@ !!!", "CRITICO");
        DecisionResponse response = decisionService.createDecision(request, testUser, null);

        assertEquals("ENTRADA_CORRUPTA", response.getBranchType());
        assertEquals("ERROR", response.getStatus());
        assertEquals(100, playthrough.getLucidity(), "Lucidity no debe cambiar con entrada corrupta");
        assertEquals(0, playthrough.getControlLevel(), "ControlLevel no debe cambiar con entrada corrupta");
        assertEquals("ACTIVA", playthrough.getStatus(), "Status no debe cambiar con entrada corrupta");
        verify(playthroughRepository, never()).save(any(Playthrough.class));
    }

    @Test
    @DisplayName("3. Impacto CRITICO: lucidity baja 40 y controlLevel sube 45, sin salirse de los limites (0-100)")
    void testImpactoCriticoConLimitesEstrictos() {
        playthrough.setLucidity(25);
        playthrough.setControlLevel(80);

        when(playthroughRepository.findById(100L)).thenReturn(Optional.of(playthrough));
        when(decisionRepository.save(any(Decision.class))).thenAnswer(invocation -> {
            Decision d = invocation.getArgument(0);
            d.setId(502L);
            return d;
        });

        DecisionRequest request = new DecisionRequest(100L, "Stefan decide obedecer y tomar el autobus.", "CRITICO");
        DecisionResponse response = decisionService.createDecision(request, testUser, null);

        // 25 - 40 = -15 -> topado en 0
        assertEquals(0, response.getLucidity(), "Lucidity debe quedar topada en 0");
        // 80 + 45 = 125 -> topado en 100
        assertEquals(100, response.getControlLevel(), "ControlLevel debe quedar topado en 100");
    }

    @Test
    @DisplayName("4. Final por controlLevel = 100 termina con ENDING_PAC_SYMBOL aunque lucidity tambien llegue a 0")
    void testFinalPorControlGanaAFinalPorLucidez() {
        playthrough.setLucidity(30);
        playthrough.setControlLevel(70);

        when(playthroughRepository.findById(100L)).thenReturn(Optional.of(playthrough));
        when(decisionRepository.save(any(Decision.class))).thenAnswer(invocation -> {
            Decision d = invocation.getArgument(0);
            d.setId(503L);
            return d;
        });

        // Impacto CRITICO: lucidity -40 (30 - 40 = 0), controlLevel +45 (70 + 45 = 100)
        DecisionRequest request = new DecisionRequest(100L, "Stefan sigue adelante con lo que el guion le indica.", "CRITICO");
        DecisionResponse response = decisionService.createDecision(request, testUser, null);

        assertEquals("FINALIZADA", response.getPlaythroughStatus());
        assertEquals("ENDING_PAC_SYMBOL", response.getEndingCode(),
                "controlLevel >= 100 debe evaluarse antes que lucidity <= 0");
    }

    @Test
    @DisplayName("5. publishEvent() se invoca exactamente 1 vez en decision normal y 0 veces en ENTRADA_CORRUPTA")
    void testPublishEventInvocacionesSegunTipo() {
        when(playthroughRepository.findById(100L)).thenReturn(Optional.of(playthrough));
        when(storyNodeRepository.findByNodeCode(any())).thenReturn(Optional.of(targetNode));
        when(decisionRepository.save(any(Decision.class))).thenAnswer(invocation -> {
            Decision d = invocation.getArgument(0);
            d.setId(504L);
            return d;
        });

        // Decisión 1: Normal
        DecisionRequest normalRequest = new DecisionRequest(100L, "Stefan acepta la oferta y sigue el guion previsto.", "LEVE");
        decisionService.createDecision(normalRequest, testUser, null);
        verify(eventPublisher, times(1)).publishEvent(any(DecisionCommittedEvent.class));

        // Decisión 2: Entrada Corrupta
        DecisionRequest corruptRequest = new DecisionRequest(100L, "1234567890 !#$%", "LEVE");
        decisionService.createDecision(corruptRequest, testUser, null);
        // Debe seguir siendo 1 invocación en total
        verify(eventPublisher, times(1)).publishEvent(any(DecisionCommittedEvent.class));
    }
}
