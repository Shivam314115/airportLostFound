import { useState, useRef, ChangeEvent, DragEvent } from 'react';
import { Camera, Upload, Trash2, Loader2 } from 'lucide-react';
import { uploadPhoto, type PhotoAttachment } from '../api/client';

export interface ImageUploadWidgetProps {
  photos: PhotoAttachment[];
  onChange: (photos: PhotoAttachment[]) => void;
  maxPhotos?: number;
  label?: string;
  hint?: string;
}

export function ImageUploadWidget({
  photos,
  onChange,
  maxPhotos = 4,
  label = 'Photographs & Evidence',
  hint = 'Upload clear photos of the item, serial numbers, distinctive marks, or purchase receipts (JPEG, PNG, WebP).',
}: ImageUploadWidgetProps) {
  const [isUploading, setIsUploading] = useState(false);
  const [uploadError, setUploadError] = useState<string | null>(null);
  const [isDragging, setIsDragging] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleFiles = async (files: FileList | File[]) => {
    if (!files || files.length === 0) return;
    setUploadError(null);

    const availableSlots = maxPhotos - photos.length;
    if (availableSlots <= 0) {
      setUploadError(`Maximum of ${maxPhotos} photos allowed.`);
      return;
    }

    const filesToUpload = Array.from(files).slice(0, availableSlots);
    setIsUploading(true);

    const newUploaded: PhotoAttachment[] = [];

    for (const file of filesToUpload) {
      if (!file.type.startsWith('image/')) {
        setUploadError(`"${file.name}" is not an image file.`);
        continue;
      }
      if (file.size > 10 * 1024 * 1024) {
        setUploadError(`"${file.name}" exceeds 10MB limit.`);
        continue;
      }

      try {
        const result = await uploadPhoto(file, file.name, 'overview');
        newUploaded.push({
          photoUrl: result.photoUrl,
          caption: file.name,
          photoType: 'overview',
        });
      } catch (err) {
        setUploadError(err instanceof Error ? err.message : 'Photo upload failed.');
      }
    }

    if (newUploaded.length > 0) {
      onChange([...photos, ...newUploaded]);
    }
    setIsUploading(false);
  };

  const onFileInputChange = (e: ChangeEvent<HTMLInputElement>) => {
    if (e.target.files) {
      handleFiles(e.target.files);
    }
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  const handleDragOver = (e: DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(true);
  };

  const handleDragLeave = (e: DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(false);
  };

  const handleDrop = (e: DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(false);
    if (e.dataTransfer?.files) {
      handleFiles(e.dataTransfer.files);
    }
  };

  const handleRemovePhoto = (index: number) => {
    const updated = photos.filter((_, i) => i !== index);
    onChange(updated);
  };

  const handleTypeChange = (index: number, type: string) => {
    const updated = [...photos];
    updated[index] = { ...updated[index], photoType: type };
    onChange(updated);
  };

  const handleCaptionChange = (index: number, caption: string) => {
    const updated = [...photos];
    updated[index] = { ...updated[index], caption };
    onChange(updated);
  };

  return (
    <div className="photo-upload-widget">
      <div className="photo-upload-header">
        <label className="photo-upload-title">
          <Camera size={16} />
          <span>{label}</span>
          <span className="photo-count-badge">
            {photos.length} / {maxPhotos}
          </span>
        </label>
        {hint && <p className="photo-upload-hint">{hint}</p>}
      </div>

      {/* Upload Zone */}
      {photos.length < maxPhotos && (
        <div
          className={`photo-dropzone ${isDragging ? 'dropzone-active' : ''}`}
          onDragOver={handleDragOver}
          onDragLeave={handleDragLeave}
          onDrop={handleDrop}
          onClick={() => fileInputRef.current?.click()}
          role="button"
          tabIndex={0}
          onKeyDown={(e) => {
            if (e.key === 'Enter' || e.key === ' ') {
              e.preventDefault();
              fileInputRef.current?.click();
            }
          }}
        >
          <input
            type="file"
            ref={fileInputRef}
            onChange={onFileInputChange}
            accept="image/png,image/jpeg,image/webp,image/gif"
            multiple
            style={{ display: 'none' }}
          />

          {isUploading ? (
            <div className="dropzone-content">
              <Loader2 className="spinner-icon" size={24} />
              <span>Uploading photo to secure storage...</span>
            </div>
          ) : (
            <div className="dropzone-content">
              <div className="dropzone-icon-circle">
                <Upload size={18} />
              </div>
              <div className="dropzone-text">
                <strong>Click to upload or drag &amp; drop photos</strong>
                <span>Supports JPG, PNG, WebP up to 10MB each</span>
              </div>
            </div>
          )}
        </div>
      )}

      {uploadError && (
        <div className="photo-upload-error" role="alert">
          {uploadError}
        </div>
      )}

      {/* Thumbnails Gallery */}
      {photos.length > 0 && (
        <div className="photo-preview-grid">
          {photos.map((p, idx) => (
            <div key={idx} className="photo-preview-card">
              <div className="photo-preview-img-wrap">
                <img
                  src={p.photoUrl}
                  alt={p.caption || `Uploaded photo ${idx + 1}`}
                  className="photo-preview-img"
                  onError={(e) => {
                    (e.currentTarget as HTMLImageElement).src =
                      'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="100" height="100" fill="%23999"><rect width="100%" height="100%" fill="%23f0f2f5"/><text x="50%" y="50%" dominant-baseline="middle" text-anchor="middle" font-size="12">Image</text></svg>';
                  }}
                />
                <button
                  type="button"
                  className="photo-remove-btn"
                  title="Remove photo"
                  onClick={(e) => {
                    e.stopPropagation();
                    handleRemovePhoto(idx);
                  }}
                >
                  <Trash2 size={14} />
                </button>
              </div>

              <div className="photo-preview-controls">
                <select
                  value={p.photoType || 'overview'}
                  onChange={(e) => handleTypeChange(idx, e.target.value)}
                  className="photo-type-select"
                >
                  <option value="overview">Overview</option>
                  <option value="serial_number">Serial Number</option>
                  <option value="damage_detail">Damage / Marks</option>
                  <option value="receipt">Receipt / Proof</option>
                  <option value="tag">Luggage Tag</option>
                  <option value="general">General</option>
                </select>

                <input
                  type="text"
                  placeholder="Optional note / caption..."
                  value={p.caption || ''}
                  onChange={(e) => handleCaptionChange(idx, e.target.value)}
                  className="photo-caption-input"
                  maxLength={100}
                />
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
