package kr.co.mcplink.domain.schedule.kr.service;

import kr.co.mcplink.domain.mcpserver.kr.entity.SynonymMapping;
import kr.co.mcplink.domain.mcpserver.kr.repository.SynonymMappingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class DataStoreKrService {

    private final SynonymMappingRepository synonymMappingRepository;

    public void saveSynonymMapping(List<String> synonyms) {

        if (synonyms == null || synonyms.isEmpty()) {
            log.warn("Cannot save empty synonym mapping");
            return;
        }

        try {
            String originalWord = synonyms.get(0);
            if (synonymMappingRepository.existsByOriginalWord(originalWord)) {
                log.debug("Synonym mapping already exists: " + originalWord);
                return;
            }

            List<String> synonymField = Collections.singletonList(originalWord);
            List<String> inputsField = new ArrayList<>();
            if (synonyms.size() > 1) {
                inputsField.addAll(synonyms.subList(1, synonyms.size()));
            }

            SynonymMapping synonymMapping = SynonymMapping.builder()
                    .input(inputsField)
                    .synonyms(synonymField)
                    .build();

            synonymMappingRepository.save(synonymMapping);
            log.info("Saved synonym mapping: {} → success", synonyms);
        } catch (DuplicateKeyException e) {
            log.debug("Synonym mapping already exists: " + synonyms.get(0));
        } catch (Exception e) {
            log.error("Error saving synonym mapping {} → {}: {}",
                    synonyms,
                    e.getClass().getSimpleName(),
                    e.getMessage());
        }
    }
}