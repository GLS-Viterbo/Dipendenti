import React, { useState, useRef } from 'react';
import { Upload, X, FileText, AlertCircle } from 'lucide-react';
import { toast } from 'react-toastify';
import { addDocument } from '../api/employees';

const UploadDocumentModal = ({ employeeId, onClose, onDocumentUploaded }) => {
    const [file, setFile] = useState(null);
    const [description, setDescription] = useState('');
    const [uploading, setUploading] = useState(false);
    const [dragActive, setDragActive] = useState(false);
    const fileInputRef = useRef(null);

    const handleDrag = (e) => {
        e.preventDefault();
        e.stopPropagation();
        if (e.type === "dragenter" || e.type === "dragover") {
            setDragActive(true);
        } else if (e.type === "dragleave") {
            setDragActive(false);
        }
    };

    const handleDrop = (e) => {
        e.preventDefault();
        e.stopPropagation();
        setDragActive(false);

        if (e.dataTransfer.files && e.dataTransfer.files[0]) {
            handleFileChange(e.dataTransfer.files[0]);
        }
    };

    const handleFileChange = (selectedFile) => {
        if (!selectedFile) return;

        // Validazione dimensione (max 10MB)
        const maxSize = 10 * 1024 * 1024; // 10MB
        if (selectedFile.size > maxSize) {
            toast.error('Il file è troppo grande. Dimensione massima: 10MB');
            return;
        }

        // Validazione tipo file
        const allowedTypes = [
            'application/pdf',
            'image/jpeg',
            'image/png',
            'image/jpg',
            'application/msword',
            'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
            'application/vnd.ms-excel',
            'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
        ];

        if (!allowedTypes.includes(selectedFile.type)) {
            toast.error('Tipo di file non supportato. Formati consentiti: PDF, JPEG, PNG, DOC, DOCX, XLS, XLSX');
            return;
        }

        setFile(selectedFile);
    };

    const handleSubmit = async (e) => {
        e.preventDefault();

        if (!file) {
            toast.error('Seleziona un file da caricare');
            return;
        }

        setUploading(true);

        try {
            // Prepara FormData per multipart/form-data
            const formData = new FormData();
            formData.append('file', file);
            formData.append('description', description);
            formData.append('employeeId', employeeId);

            for (const [key, value] of formData.entries()) {
                console.log(key, value);
            }

            await addDocument(formData)

            toast.success('Documento caricato con successo!');
            onDocumentUploaded();
            onClose();
        } catch (error) {
            console.error('Errore upload documento:', error);
            toast.error('Errore durante il caricamento del documento');
        } finally {
            setUploading(false);
        }
    };

    const removeFile = () => {
        setFile(null);
        if (fileInputRef.current) {
            fileInputRef.current.value = '';
        }
    };

    const formatFileSize = (bytes) => {
        if (bytes < 1024) return bytes + ' B';
        if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
        return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
    };

    const getFileIcon = (type) => {
        if (type.includes('pdf')) return '📄';
        if (type.includes('image')) return '🖼️';
        if (type.includes('word') || type.includes('document')) return '📝';
        if (type.includes('excel') || type.includes('spreadsheet')) return '📊';
        return '📎';
    };

    return (
        <div
            className="modal show d-block"
            style={{ backgroundColor: 'rgba(0,0,0,0.5)' }}
            onClick={onClose}
        >
            <div
                className="modal-dialog modal-dialog-centered"
                onClick={(e) => e.stopPropagation()}
            >
                <div className="modal-content">
                    <div className="modal-header border-bottom">
                        <h5 className="modal-title fw-semibold">
                            <Upload size={20} className="me-2" />
                            Carica Documento
                        </h5>
                        <button
                            type="button"
                            className="btn-close"
                            onClick={onClose}
                            disabled={uploading}
                        ></button>
                    </div>

                    <form onSubmit={handleSubmit}>
                        <div className="modal-body">
                            {/* Drag & Drop Area */}
                            <div
                                className={`border-2 border-dashed rounded p-4 text-center mb-3 ${dragActive ? 'border-primary bg-primary bg-opacity-10' : 'border-secondary'
                                    }`}
                                onDragEnter={handleDrag}
                                onDragLeave={handleDrag}
                                onDragOver={handleDrag}
                                onDrop={handleDrop}
                            >
                                {!file ? (
                                    <>
                                        <Upload size={48} className="text-muted mb-3" />
                                        <p className="mb-2">
                                            Trascina un file qui o{' '}
                                            <button
                                                type="button"
                                                className="btn btn-link p-0 text-decoration-none"
                                                onClick={() => fileInputRef.current?.click()}
                                            >
                                                sfoglia
                                            </button>
                                        </p>
                                        <small className="text-muted">
                                            Formati supportati: PDF, JPEG, PNG, DOC, DOCX, XLS, XLSX
                                            <br />
                                            Dimensione massima: 10MB
                                        </small>
                                        <input
                                            ref={fileInputRef}
                                            type="file"
                                            className="d-none"
                                            onChange={(e) => handleFileChange(e.target.files[0])}
                                            accept=".pdf,.jpg,.jpeg,.png,.doc,.docx,.xls,.xlsx"
                                        />
                                    </>
                                ) : (
                                    <div className="d-flex align-items-center justify-content-between bg-light rounded p-3">
                                        <div className="d-flex align-items-center gap-3">
                                            <span style={{ fontSize: '2rem' }}>
                                                {getFileIcon(file.type)}
                                            </span>
                                            <div className="text-start">
                                                <div className="fw-semibold">{file.name}</div>
                                                <small className="text-muted">
                                                    {formatFileSize(file.size)}
                                                </small>
                                            </div>
                                        </div>
                                        <button
                                            type="button"
                                            className="btn btn-sm btn-outline-danger"
                                            onClick={removeFile}
                                            disabled={uploading}
                                        >
                                            <X size={16} />
                                        </button>
                                    </div>
                                )}
                            </div>

                            {/* Description Field */}
                            <div className="mb-3">
                                <label htmlFor="description" className="form-label fw-semibold">
                                    Descrizione <small className="text-muted">(opzionale)</small>
                                </label>
                                <textarea
                                    id="description"
                                    className="form-control"
                                    rows="3"
                                    placeholder="Aggiungi una descrizione del documento..."
                                    value={description}
                                    onChange={(e) => setDescription(e.target.value)}
                                    disabled={uploading}
                                    maxLength={500}
                                />
                                <small className="text-muted">
                                    {description.length}/500 caratteri
                                </small>
                            </div>

                            {/* Info Alert */}
                            <div className="alert alert-info d-flex align-items-start gap-2 mb-0">
                                <AlertCircle size={20} className="flex-shrink-0 mt-1" />
                                <small>
                                    Il documento sarà associato al dipendente e potrà essere scaricato
                                    in qualsiasi momento dalla sezione Documenti.
                                </small>
                            </div>
                        </div>

                        <div className="modal-footer border-top">
                            <button
                                type="button"
                                className="btn btn-outline-secondary"
                                onClick={onClose}
                                disabled={uploading}
                            >
                                Annulla
                            </button>
                            <button
                                type="submit"
                                className="btn btn-primary d-flex align-items-center gap-2"
                                disabled={!file || uploading}
                            >
                                {uploading ? (
                                    <>
                                        <span className="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span>
                                        <span>Caricamento...</span>
                                    </>
                                ) : (
                                    <>
                                        <Upload size={16} />
                                        <span>Carica Documento</span>
                                    </>
                                )}
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    );
};

export default UploadDocumentModal;