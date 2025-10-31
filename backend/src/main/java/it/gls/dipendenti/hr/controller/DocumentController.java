package it.gls.dipendenti.hr.controller;

import it.gls.dipendenti.hr.model.EmployeeDocument;
import it.gls.dipendenti.hr.repository.EmployeeDocumentRepository;
import it.gls.dipendenti.hr.service.DocumentStorageService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentStorageService storageService;
    private final EmployeeDocumentRepository documentRepository;

    public DocumentController(DocumentStorageService storageService,
                              EmployeeDocumentRepository documentRepository) {
        this.storageService = storageService;
        this.documentRepository = documentRepository;
    }

    /**
     * Upload a document for an employee
     */
    @PostMapping("/upload")
    public ResponseEntity<EmployeeDocument> uploadDocument(
            @RequestParam("employeeId") Long employeeId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "description", required = false) String description) {

        EmployeeDocument document = storageService.storeDocument(employeeId, file, description);
        return ResponseEntity.ok(document);
    }

    /**
     * Get all documents for an employee
     */
    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<EmployeeDocument>> getEmployeeDocuments(@PathVariable Long employeeId) {
        List<EmployeeDocument> documents = documentRepository.findByEmployeeId(employeeId);
        return ResponseEntity.ok(documents);
    }

    /**
     * Download a specific document
     */
    @GetMapping("/{documentId}/download")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long documentId) {
        EmployeeDocument document = documentRepository.findById(documentId)
                .orElse(null);

        if (document == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            Path file = storageService.loadDocument(document);
            Resource resource = new UrlResource(file.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            // Determina content type
            String contentType = document.mimeType() != null
                    ? document.mimeType()
                    : "application/octet-stream";

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + document.fileName() + "\"")
                    .body(resource);

        } catch (MalformedURLException e) {
            return ResponseEntity.internalServerError().build();
        }
    }


    /**
     * Delete a document
     */
    @DeleteMapping("/{documentId}")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long documentId) {
        boolean deleted = storageService.deleteDocument(documentId);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    /**
     * Get document metadata
     */
    @GetMapping("/{documentId}")
    public ResponseEntity<EmployeeDocument> getDocument(@PathVariable Long documentId) {
        return documentRepository.findById(documentId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}