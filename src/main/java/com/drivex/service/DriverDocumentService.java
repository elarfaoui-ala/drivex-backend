package com.drivex.service;

import com.drivex.dto.Dtos.*;
import com.drivex.entity.Driver;
import com.drivex.entity.DriverDocument;
import com.drivex.exception.ApiException;
import com.drivex.repository.DriverDocumentRepository;
import com.drivex.repository.DriverRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DriverDocumentService {

    private final DriverDocumentRepository documentRepository;
    private final DriverRepository         driverRepository;

    public List<DocumentResponse> getDocuments(String driverId) {
        return documentRepository.findByDriverId(driverId)
            .stream()
            .map(DocumentResponse::from)
            .toList();
    }

    @Transactional
    public DocumentResponse uploadDocument(String driverId, DocumentUploadRequest req) {
        Driver driver = driverRepository.findById(driverId)
            .orElseThrow(() -> ApiException.notFound("Driver", driverId));

        var doc = DriverDocument.builder()
            .id(UUID.randomUUID().toString())
            .driver(driver)
            .type(req.type())
            .fileUrl(req.fileUrl())
            .fileName(req.fileName())
            .expiryDate(req.expiryDate())
            .build();

        documentRepository.save(doc);
        log.info("Document {} uploaded for driver {}: {}", doc.getId(), driverId, req.type());
        return DocumentResponse.from(doc);
    }

    @Transactional
    public void deleteDocument(String driverId, String documentId) {
        DriverDocument doc = documentRepository.findById(documentId)
            .orElseThrow(() -> ApiException.notFound("Document", documentId));

        if (!doc.getDriver().getId().equals(driverId)) {
            throw ApiException.forbidden("Document does not belong to this driver");
        }

        documentRepository.delete(doc);
        log.info("Document {} deleted for driver {}", documentId, driverId);
    }
}
