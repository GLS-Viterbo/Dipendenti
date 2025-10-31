package it.gls.dipendenti.hr.service;

import it.gls.dipendenti.hr.model.EmployeeDocument;
import it.gls.dipendenti.hr.repository.EmployeeDocumentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.UUID;

@Service
public class DocumentStorageService {

    private final EmployeeDocumentRepository documentRepository;
    private final Path rootLocation;

    public DocumentStorageService(
            EmployeeDocumentRepository documentRepository,
            @Value("${document.storage.path:./documents}") String storagePath) {
        this.documentRepository = documentRepository;
        this.rootLocation = Paths.get(storagePath);
        initStorage();
    }

    /**
     * Initialize storage directory if it doesn't exist
     */
    private void initStorage() {
        try {
            if (!Files.exists(rootLocation)) {
                Files.createDirectories(rootLocation);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage directory", e);
        }
    }

    /**
     * Store a document file and save metadata to database
     * @param employeeId the employee id
     * @param file the uploaded file
     * @param description optional description
     * @return saved EmployeeDocument
     */
    public EmployeeDocument storeDocument(Long employeeId, MultipartFile file, String description) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Cannot store empty file");
        }

        try {
            // Create employee-specific subdirectory
            Path employeeDir = rootLocation.resolve(String.valueOf(employeeId));
            if (!Files.exists(employeeDir)) {
                Files.createDirectories(employeeDir);
            }

            // Generate unique filename to avoid conflicts
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : "";
            String uniqueFilename = UUID.randomUUID().toString() + extension;

            // Store file
            Path destinationFile = employeeDir.resolve(uniqueFilename);
            Files.copy(file.getInputStream(), destinationFile, StandardCopyOption.REPLACE_EXISTING);

            // Save metadata to database with RELATIVE path
            String relativePath = employeeId + "/" + uniqueFilename;
            EmployeeDocument document = new EmployeeDocument(
                    null,
                    employeeId,
                    originalFilename,
                    relativePath,  // Store relative path, not absolute
                    file.getContentType(),
                    description,
                    Instant.now(),
                    false
            );

            return documentRepository.save(document);

        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }
    }

    /**
     * Load a document file from storage
     * @param document the document metadata
     * @return Path to the file
     */
    public Path loadDocument(EmployeeDocument document) {
        Path file = rootLocation.resolve(document.filePath());
        if (!Files.exists(file)) {
            throw new RuntimeException("File not found: " + document.fileName());
        }
        return file;
    }

    /**
     * Delete a document file and mark as deleted in database
     * @param documentId the document id
     * @return true if successful
     */
    public boolean deleteDocument(Long documentId) {
        return documentRepository.findById(documentId)
                .map(document -> {
                    try {
                        // Delete physical file
                        Path file = rootLocation.resolve(document.filePath());
                        if (Files.exists(file)) {
                            Files.delete(file);
                        }
                        // Soft delete in database
                        return documentRepository.delete(documentId);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to delete file", e);
                    }
                })
                .orElse(false);
    }

    /**
     * Get the absolute path for a document
     * @param relativePath the relative path stored in database
     * @return absolute Path
     */
    public Path getAbsolutePath(String relativePath) {
        return rootLocation.resolve(relativePath);
    }

    /**
     * Check if a document file exists
     * @param document the document metadata
     * @return true if file exists
     */
    public boolean fileExists(EmployeeDocument document) {
        Path file = rootLocation.resolve(document.filePath());
        return Files.exists(file);
    }
}