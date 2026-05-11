package com.drivex.controller;

import com.drivex.dto.Dtos.*;
import com.drivex.service.DriverDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/drivers/{driverId}/documents")
@RequiredArgsConstructor
@Tag(name = "Documents", description = "Driver document management (license, insurance, etc.)")
@SecurityRequirement(name = "bearerAuth")
public class DocumentController {

    private final DriverDocumentService documentService;

    @GetMapping
    @Operation(summary = "List all documents for a driver")
    public List<DocumentResponse> getDocuments(@PathVariable String driverId) {
        return documentService.getDocuments(driverId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Upload a new document (driver's license, insurance, etc.)")
    public DocumentResponse uploadDocument(
        @PathVariable String driverId,
        @Valid @RequestBody DocumentUploadRequest req
    ) {
        return documentService.uploadDocument(driverId, req);
    }

    @DeleteMapping("/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a driver document")
    public void deleteDocument(
        @PathVariable String driverId,
        @PathVariable String documentId
    ) {
        documentService.deleteDocument(driverId, documentId);
    }
}
